package com.vivu.booking.service.impl;


import com.vivu.booking.common.PageResponse;
import com.vivu.booking.config.MinioConfig;
import com.vivu.booking.dao.RoomDao;
import com.vivu.booking.dao.RoomImageDao;
import com.vivu.booking.dto.request.RoomCreateRequest;
import com.vivu.booking.dto.request.RoomUpdateRequest;
import com.vivu.booking.dto.response.RoomMediaItem;
import com.vivu.booking.dto.response.RoomResponse;
import com.vivu.booking.entity.Room;
import com.vivu.booking.entity.RoomImage;
import com.vivu.booking.enums.MediaTypeEnum;
import com.vivu.booking.enums.RoomStatus;
import com.vivu.booking.enums.RoomType;
import com.vivu.booking.exception.BusinessException;
import com.vivu.booking.exception.ResourceNotFoundException;
import com.vivu.booking.mapper.RoomMapper;
import com.vivu.booking.service.RoomService;
import jakarta.servlet.http.Part;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

public class RoomServiceImpl implements RoomService {

    private static final Logger log = LoggerFactory.getLogger(RoomServiceImpl.class);
    private final RoomDao roomDao;
    private final RoomImageDao roomImageDao;

    public RoomServiceImpl(RoomDao roomDao, RoomImageDao roomImageDao) { this.roomDao = roomDao; this.roomImageDao = roomImageDao; }
    public RoomServiceImpl(RoomDao roomDao) { this(roomDao, new RoomImageDao()); }
    public RoomServiceImpl() { this(new RoomDao(), new RoomImageDao()); }

    /** Chuyển RoomImage -> RoomMediaItem (id + url + mediaType dạng string). */
    private static List<RoomMediaItem> toMediaItems(List<RoomImage> entities) {
        return entities.stream().map(e -> RoomMediaItem.builder()
                .id(e.getId())
                .url(e.getUrl())
                .mediaType(e.getMediaType() == null ? "IMAGE" : e.getMediaType().name())
                .build()).toList();
    }

    /** Lấy ảnh/video của một phòng — trả về mediaItems để RoomMapper tách images/videos. */
    private List<RoomMediaItem> loadMedia(Long roomId) {
        return toMediaItems(roomImageDao.findByRoomIdOrdered(roomId));
    }

    /** Trích objectName từ MinIO URL: endpoint/bucket/objectName -> objectName. */
    private static String extractObjectName(String url) {
        if (url == null || url.isBlank()) return null;
        try {
            String bucket = MinioConfig.getBucket();
            // URL = endpoint/bucket/objectName — objectName là phần còn lại sau bucket/
            int idx = url.indexOf("/" + bucket + "/");
            if (idx >= 0) return url.substring(idx + bucket.length() + 2);
            // fallback: lấy phần sau host/path đầu
            return url.substring(url.indexOf("/", url.indexOf("://") + 3) + 1).replaceFirst("^[^/]+/", "");
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public RoomResponse create(RoomCreateRequest req) {
        if (roomDao.existsByCode(req.getCode())) {
            throw new BusinessException(409, "Room code already exists: " + req.getCode());
        }
        Room entity = RoomMapper.toEntity(req);
        roomDao.save(entity);
        log.info("Room created id={} code={}", entity.getId(), entity.getCode());
        return RoomMapper.toResponse(entity);
    }

    @Override
    public RoomResponse getById(Long id) {
        Room r = roomDao.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + id));
        List<RoomMediaItem> media = loadMedia(id);
        return RoomMapper.toResponse(r, media.isEmpty() ? null : media);
    }

    @Override
    public PageResponse<RoomResponse> list(RoomType type, RoomStatus status, String keyword, int page, int size) {
        return list(type, status, keyword, null, null, null, page, size, null, null);
    }

    @Override
    public PageResponse<RoomResponse> list(RoomType type, RoomStatus status, String keyword,
                                           Long minPrice, Long maxPrice, Integer minCapacity,
                                           int page, int size, String sortBy, String sortDir) {
        // chong tham so vo hieu: chi sort tren cot hop le, mac dinh id desc (moi -> cu)
        String col = switch (sortBy == null ? "" : sortBy) {
            case "pricePerNight", "price" -> "pricePerNight";
            case "name" -> "name";
            case "createdAt" -> "createdAt";
            default -> null;
        };
        String dir = "asc".equalsIgnoreCase(sortDir) ? "asc" : "desc";
        long total = roomDao.countSearch(type, status, keyword, minPrice, maxPrice, minCapacity);
        List<Room> rooms = roomDao.search(type, status, keyword, minPrice, maxPrice, minCapacity, page, size, col, dir);
        // Batch load media cho tất cả phòng trong trang (tránh N+1 query)
        List<RoomResponse> content = rooms.stream().map(r -> {
            List<RoomMediaItem> media = loadMedia(r.getId());
            return RoomMapper.toResponse(r, media.isEmpty() ? null : media);
        }).toList();
        return PageResponse.of(content, page, size, total);
    }

    @Override
    public RoomResponse update(Long id, RoomUpdateRequest req) {
        Room r = roomDao.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + id));
        if (req.getName() != null) r.setName(req.getName());
        if (req.getType() != null) r.setType(req.getType());
        if (req.getStatus() != null) r.setStatus(req.getStatus());
        if (req.getCapacity() != null) r.setCapacity(req.getCapacity());
        if (req.getPricePerNight() != null) r.setPricePerNight(req.getPricePerNight());
        if (req.getDescription() != null) r.setDescription(req.getDescription());
        if (req.getImageUrl() != null) r.setImageUrl(req.getImageUrl());
        if (req.getActive() != null) r.setActive(req.getActive());
        Room merged = roomDao.update(r);
        List<RoomMediaItem> media = loadMedia(id);
        return RoomMapper.toResponse(merged, media.isEmpty() ? null : media);
    }

    @Override
    public void delete(Long id) {
        Room r = roomDao.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + id));
        r.setActive(false);
        roomDao.update(r);
    }

    @Override
    public String uploadMedia(Long id, Part file) {
        Room r = roomDao.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + id));
        String contentType = file.getContentType();
        if (contentType == null || !(contentType.startsWith("image/") || contentType.startsWith("video/"))) {
            throw new BusinessException(400, "Chỉ chấp nhận file ảnh (jpg/png/webp...) hoặc video (mp4/mov...)");
        }
        MediaTypeEnum mediaType = contentType.startsWith("video/") ? MediaTypeEnum.VIDEO : MediaTypeEnum.IMAGE;
        boolean isVideo = mediaType == MediaTypeEnum.VIDEO;
        try {
            String bucket = MinioConfig.getBucket();
            MinioConfig.createBucket(bucket);
            MinioConfig.setPublic(bucket);
            String original = file.getSubmittedFileName() == null ? (isVideo ? "video.mp4" : "img") : file.getSubmittedFileName();
            String folder = isVideo ? "rooms/videos" : "rooms/images";
            String objectName = String.format("%s/%d/%s_%s", folder, r.getId(), UUID.randomUUID(), original);
            try (InputStream in = file.getInputStream()) {
                MinioConfig.upload(bucket, objectName, in, file.getSize(), contentType);
            }
            String url = MinioConfig.getObjectUrl(bucket, objectName);
            // sortOrder = số media hiện tại => media mới luôn đứng cuối carousel/playlist
            int nextOrder = roomImageDao.findByRoomIdOrdered(r.getId()).size();
            roomImageDao.save(RoomImage.builder().room(r).url(url).mediaType(mediaType).sortOrder(nextOrder).build());
            // Nếu là ảnh đầu tiên và room chưa có imageUrl -> tự đặt làm bìa
            if (!isVideo && (r.getImageUrl() == null || r.getImageUrl().isBlank())) {
                r.setImageUrl(url);
                roomDao.update(r);
            }
            log.info("Room media uploaded room={} type={} url={}", r.getId(), mediaType, url);
            return url;
        } catch (BusinessException be) {
            throw be;
        } catch (Exception e) {
            log.error("Upload room media failed room={}", id, e);
            throw new BusinessException(500, "Tải file lên thất bại, vui lòng thử lại");
        }
    }

    @Override
    public void deleteMedia(Long roomId, Long mediaId) {
        RoomImage media = roomImageDao.findByRoomIdAndId(roomId, mediaId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy media " + mediaId + " của phòng " + roomId));
        // Xoá object trên MinIO trước (best-effort)
        String objectName = extractObjectName(media.getUrl());
        if (objectName != null) {
            try {
                MinioConfig.removeObject(MinioConfig.getBucket(), objectName);
            } catch (Exception e) {
                log.warn("Remove MinIO object failed {}: {}", objectName, e.getMessage());
            }
        }
        boolean wasCover = media.getMediaType() == MediaTypeEnum.IMAGE
                && roomDao.findById(roomId).map(Room::getImageUrl).map(u -> u.equals(media.getUrl())).orElse(false);
        roomImageDao.deleteById(mediaId);
        // Nếu ảnh bị xoá là ảnh bìa -> đổi sang ảnh IMAGE kế tiếp (nếu còn)
        if (wasCover) {
            List<RoomImage> remaining = roomImageDao.findByRoomIdAndType(roomId, MediaTypeEnum.IMAGE);
            roomDao.findById(roomId).ifPresent(r -> {
                r.setImageUrl(remaining.isEmpty() ? null : remaining.get(0).getUrl());
                roomDao.update(r);
            });
        }
        log.info("Room media deleted room={} mediaId={} type={}", roomId, mediaId, media.getMediaType());
    }
}
