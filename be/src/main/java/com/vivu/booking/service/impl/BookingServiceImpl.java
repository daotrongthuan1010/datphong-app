package com.vivu.booking.service.impl;

import com.vivu.booking.common.PageResponse;
import com.vivu.booking.dao.BookingDao;
import com.vivu.booking.dao.RoomDao;
import com.vivu.booking.dao.UsersDao;
import com.vivu.booking.dao.VoucherDao;
import com.vivu.booking.dto.request.BookingCreateRequest;
import com.vivu.booking.dto.response.BookingResponse;
import com.vivu.booking.entity.Booking;
import com.vivu.booking.entity.Room;
import com.vivu.booking.entity.User;
import com.vivu.booking.entity.Voucher;
import com.vivu.booking.enums.BookingStatusType;
import com.vivu.booking.exception.BusinessException;
import com.vivu.booking.exception.ResourceNotFoundException;
import com.vivu.booking.mapper.BookingMapper;
import com.vivu.booking.service.BookingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

public class BookingServiceImpl implements BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingServiceImpl.class);
    private static final int HOLD_MINUTES = 15;

    private final BookingDao bookingDao;
    private final RoomDao roomDao;
    private final UsersDao usersDao;
    private final VoucherDao voucherDao;

    public BookingServiceImpl(BookingDao bookingDao, RoomDao roomDao, UsersDao usersDao, VoucherDao voucherDao) {
        this.bookingDao = bookingDao;
        this.roomDao = roomDao;
        this.usersDao = usersDao;
        this.voucherDao = voucherDao;
    }

    public BookingServiceImpl() {
        this(new BookingDao(), new RoomDao(), new UsersDao(), new VoucherDao());
    }

    @Override
    public BookingResponse create(Long userId, BookingCreateRequest req) {
        User user = usersDao.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        Room room = roomDao.findById(req.getRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + req.getRoomId()));

        if (Boolean.FALSE.equals(room.getActive())) {
            throw new BusinessException(400, "Phòng đã ngừng phục vụ");
        }
        if (req.getGuestsCount() > room.getCapacity()) {
            throw new BusinessException(400, "Số khách (" + req.getGuestsCount() + ") vượt sức chứa phòng (" + room.getCapacity() + ")");
        }
        if (bookingDao.existsOverlap(room.getId(), req.getCheckinDate(), req.getCheckoutDate())) {
            throw new BusinessException(409, "Phòng đã được đặt trong khoảng thời gian này, vui lòng chọn ngày khác");
        }

        long nights = ChronoUnit.DAYS.between(req.getCheckinDate(), req.getCheckoutDate());
        if (nights <= 0) throw new BusinessException(400, "Ngày checkout phải sau checkin");

        Voucher voucher = null;
        if (req.getVoucherCode() != null && !req.getVoucherCode().isBlank()) {
            voucher = voucherDao.findByCode(req.getVoucherCode().trim().toUpperCase())
                    .orElseThrow(() -> new BusinessException(404, "Voucher không tồn tại: " + req.getVoucherCode()));
            // Don gian: voucher hop le neu id ton tai — cac giam gia ao dung tang voucher them truong trang thai.
        }

        BigDecimal pricePerNight = BigDecimal.valueOf(room.getPricePerNight());
        BigDecimal total = pricePerNight.multiply(BigDecimal.valueOf(nights));

        Booking booking = Booking.builder()
                .bookingCode("BV" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .user(user)
                .room(room)
                .checkinDate(req.getCheckinDate())
                .checkoutDate(req.getCheckoutDate())
                .guestsCount(req.getGuestsCount())
                .status(BookingStatusType.CONFIRMED)
                .totalPrice(total)
                .currency("VND")
                .voucher(voucher)
                .loyaltyDiscountPercent(BigDecimal.ZERO)
                .build();

        bookingDao.save(booking);
        log.info("Booking created id={} code={} user={} room={}", booking.getId(), booking.getBookingCode(), userId, room.getId());
        return BookingMapper.toResponse(bookingDao.findByIdWithRoom(booking.getId())
                .orElse(booking));
    }

    @Override
    public PageResponse<BookingResponse> listByUser(Long userId, int page, int size) {
        long total = bookingDao.countByUserId(userId);
        List<BookingResponse> content = bookingDao.findByUserId(userId, page, size)
                .stream().map(BookingMapper::toResponse).toList();
        return PageResponse.of(content, page, size, total);
    }

    @Override
    public BookingResponse getById(Long userId, Long id) {
        Booking b = bookingDao.findByIdWithRoom(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + id));
        if (!b.getUser().getId().equals(userId)) {
            throw new BusinessException(403, "Bạn không có quyền xem đặt phòng này");
        }
        return BookingMapper.toResponse(b);
    }

    @Override
    public BookingResponse cancel(Long userId, Long id) {
        Booking b = bookingDao.findByIdWithRoom(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + id));
        if (!b.getUser().getId().equals(userId)) {
            throw new BusinessException(403, "Bạn không có quyền hủy đặt phòng này");
        }
        if (b.getStatus() == BookingStatusType.CANCELLED || b.getStatus() == BookingStatusType.COMPLETED) {
            throw new BusinessException(400, "Đặt phòng đã ở trạng thái không thể hủy: " + b.getStatus());
        }
        b.setStatus(BookingStatusType.CANCELLED);
        Booking merged = bookingDao.update(b);
        return BookingMapper.toResponse(merged);
    }

    @Override
    public boolean isAvailable(Long roomId, LocalDate checkin, LocalDate checkout) {
        return !bookingDao.existsOverlap(roomId, checkin, checkout);
    }
}
