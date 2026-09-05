package com.vivu.booking.service.impl;

import com.vivu.booking.config.MinioConfig;
import com.vivu.booking.dao.BookingDao;
import com.vivu.booking.dao.ReviewDao;
import com.vivu.booking.dao.ReviewMediaDao;
import com.vivu.booking.dto.request.ReviewCreateRequest;
import com.vivu.booking.dto.response.ReviewResponse;
import com.vivu.booking.entity.Booking;
import com.vivu.booking.entity.Review;
import com.vivu.booking.entity.ReviewMedia;
import com.vivu.booking.enums.BookingStatusType;
import com.vivu.booking.enums.MediaTypeEnum;
import com.vivu.booking.exception.BusinessException;
import com.vivu.booking.exception.ResourceNotFoundException;
import com.vivu.booking.mapper.ReviewMapper;
import com.vivu.booking.service.ReviewService;
import com.vivu.booking.utils.ValidationUtils;
import jakarta.servlet.http.Part;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class ReviewServiceImpl implements ReviewService {

    private static final Logger log = LoggerFactory.getLogger(ReviewServiceImpl.class);

    private final ReviewDao reviewDao;
    private final ReviewMediaDao mediaDao;
    private final BookingDao bookingDao;

    public ReviewServiceImpl(ReviewDao reviewDao, ReviewMediaDao mediaDao, BookingDao bookingDao) {
        this.reviewDao = reviewDao;
        this.mediaDao = mediaDao;
        this.bookingDao = bookingDao;
    }

    public ReviewServiceImpl() {
        this(new ReviewDao(), new ReviewMediaDao(), new BookingDao());
    }

    @Override
    public Map<String, Object> listByRoom(Long roomId, int page, int size) {
        long total = reviewDao.countVisibleByRoomId(roomId);
        List<Review> reviews = reviewDao.findVisibleByRoomId(roomId, page, size);

        // Gom media 1 query (tránh N+1)
        List<Long> ids = reviews.stream().map(Review::getId).toList();
        Map<Long, List<ReviewMedia>> mediaByReview = mediaDao.findByReviewIds(ids).stream()
                .collect(Collectors.groupingBy(m -> m.getReview().getId()));

        List<ReviewResponse> content = reviews.stream()
                .map(r -> ReviewMapper.toResponse(r, mediaByReview.getOrDefault(r.getId(), List.of())))
                .toList();

        Double avg = reviewDao.avgRatingByRoom(roomId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content", content);
        result.put("page", page);
        result.put("size", size);
        result.put("totalElements", total);
        result.put("totalPages", size == 0 ? 0 : (int) Math.ceil((double) total / size));
        result.put("avgRating", avg == null ? 0 : Math.round(avg * 10) / 10.0);
        return result;
    }

    @Override
    public ReviewResponse create(Long userId, ReviewCreateRequest req, List<Part> mediaParts) {
        ValidationUtils.validate(req);
        if (req.getBookingId() == null) {
            throw new BusinessException(400, "Thiếu bookingId — review phải gắn với một đặt phòng");
        }

        Booking booking = bookingDao.findByIdWithRoom(req.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đặt phòng: " + req.getBookingId()));

        if (!booking.getUser().getId().equals(userId)) {
            throw new BusinessException(403, "Bạn chỉ được đánh giá đặt phòng của chính mình");
        }
        if (booking.getStatus() != BookingStatusType.CONFIRMED && booking.getStatus() != BookingStatusType.COMPLETED) {
            throw new BusinessException(409, "Chỉ đánh giá được đặt phòng đã xác nhận hoặc hoàn thành");
        }
        if (reviewDao.findByBookingId(booking.getId()).isPresent()) {
            throw new BusinessException(409, "Đặt phòng này đã được đánh giá trước đó");
        }

        // Upload media lên MinIO trước (fail thì không tạo review dở)
        List<String[]> uploaded = new ArrayList<>(); // [url, type]
        String bucket = MinioConfig.getBucket();
        try {
            MinioConfig.createBucket(bucket);
            MinioConfig.setPublic(bucket);
            String folder = String.format("%d/%02d/%02d/reviews",
                    LocalDate.now().getYear(), LocalDate.now().getMonthValue(), LocalDate.now().getDayOfMonth());
            for (Part part : mediaParts) {
                if (part == null || part.getSize() == 0) continue;
                MediaTypeEnum type = part.getContentType() != null && part.getContentType().startsWith("video/")
                        ? MediaTypeEnum.VIDEO : MediaTypeEnum.IMAGE;
                String original = part.getSubmittedFileName() == null ? "media" : part.getSubmittedFileName();
                String objectName = folder + "/" + UUID.randomUUID() + "_" + original;
                try (InputStream in = part.getInputStream()) {
                    MinioConfig.upload(bucket, objectName, in, part.getSize(), part.getContentType());
                }
                uploaded.add(new String[]{MinioConfig.getObjectUrl(bucket, objectName), type.name()});
            }
        } catch (BusinessException be) {
            throw be;
        } catch (Exception e) {
            throw new BusinessException(500, "Tải media lên MinIO thất bại: " + e.getMessage());
        }

        Review review = ReviewMapper.toEntity(req);
        review.setBooking(booking);
        review.setUser(booking.getUser());
        review.setRoom(booking.getRoom());
        reviewDao.save(review);

        List<ReviewMedia> mediaEntities = new ArrayList<>();
        for (String[] u : uploaded) {
            ReviewMedia m = ReviewMedia.builder()
                    .review(review)
                    .url(u[0])
                    .mediaType(MediaTypeEnum.valueOf(u[1]))
                    .build();
            mediaDao.save(m);
            mediaEntities.add(m);
        }
        log.info("Review created id={} booking={} media={}", review.getId(), booking.getBookingCode(), mediaEntities.size());
        return ReviewMapper.toResponse(review, mediaEntities);
    }
}
