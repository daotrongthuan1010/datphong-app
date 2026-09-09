package com.vivu.booking.dao;

import com.vivu.booking.entity.Booking;
import com.vivu.booking.enums.BookingStatusType;
import org.hibernate.Session;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class BookingDao extends BaseDao<Booking, Long> {

    /**
     * Các trạng thái "đang chiếm phòng" — dùng cho mọi truy vấn chống chồng lấn.
     * HOLD phải có mặt: user đang trong 15 phút giữ chỗ, phòng vẫn bị coi là chiếm.
     * Chỉ khi HOLD hết hạn (scheduler chuyển sang EXPIRED) thì lịch mới được trả.
     */
    public static final List<BookingStatusType> ACTIVE_STATUSES =
            List.of(BookingStatusType.HOLD, BookingStatusType.PENDING_PAYMENT, BookingStatusType.CONFIRMED);

    /**
     * Các trạng thái <b>đang bị đếm ngược giữ chỗ</b> — job hết hạn phải quét cả hai.
     *
     * <p>Bản cũ chỉ có {@code HOLD}, nên booking đã chuyển sang {@code PENDING_PAYMENT}
     * (khách bấm "Thanh toán" rồi bỏ dở trang cổng) không bao giờ hết hạn: các đêm bị
     * BLOCKED vĩnh viễn, phòng trống mà không ai đặt được, và không có lỗi nào được log.
     * Cả hai trạng thái đều dùng chung một mốc {@code hold_expires_at}.
     */
    public static final List<BookingStatusType> HOLDING_STATUSES =
            List.of(BookingStatusType.HOLD, BookingStatusType.PENDING_PAYMENT);

    public BookingDao() {
        super(Booking.class);
    }

    /**
     * Mở 1 transaction Hibernate và trả quyền điều khiển Session cho caller.
     *
     * <p>{@link BaseDao#tx} là protected (chỉ DAO dùng nội bộ). Nghiệp vụ đặt phòng cần
     * nhiều DAO chung 1 transaction (Booking + RoomCalendar + Room) nên Service phải
     * tự viết "cả khối" trong một Session duy nhất — đây là cổng public để làm việc đó.
     * Transaction được commit khi work trả về bình thường, rollback khi có RuntimeException.
     */
    public <R> R inTransaction(Function<Session, R> work) {
        return tx(work);
    }

    public Optional<Booking> findByBookingCode(String code) {
        return read(s -> s.createQuery("from Booking where bookingCode = :code", Booking.class)
                .setParameter("code", code).uniqueResultOptional());
    }

    /** Lấy kèm user/room/voucher để map response sau khi session đóng (tránh LazyInitializationException). */
    public Optional<Booking> findByIdWithRoom(Long id) {
        return read(s -> s.createQuery("""
                select distinct b from Booking b
                left join fetch b.room
                left join fetch b.user
                left join fetch b.voucher
                where b.id = :id
                """, Booking.class)
                .setParameter("id", id).uniqueResultOptional());
    }

    public List<Booking> findByUserId(Long userId, int page, int size) {
        return read(s -> s.createQuery("""
                select distinct b from Booking b
                left join fetch b.room
                left join fetch b.user
                left join fetch b.voucher
                where b.user.id = :userId
                order by b.id desc
                """, Booking.class)
                .setParameter("userId", userId)
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList());
    }

    public long countByUserId(Long userId) {
        return read(s -> s.createQuery("select count(b) from Booking b where b.user.id = :userId", Long.class)
                .setParameter("userId", userId).getSingleResult());
    }

    /**
     * Đếm booking đang chiếm phòng (HOLD/PENDING_PAYMENT/CONFIRMED) để chung khoảng [checkin, checkout).
     *
     * <p>Hai điều kiện overlap chuẩn cho khoảng bán mở [a, b): {@code existing.checkin < b}
     * và {@code existing.checkout > a}. Dùng 1 đếm (không boolean) để caller log số lượng khi trả 409.
     *
     * <p>Bản cũ set thêm tham số {@code hold}/{@code now} không có trong HQL — Hibernate 6
     * ném IllegalArgumentException("Could not locate named parameter") nên method này luôn fail.
     */
    public long countOverlap(Long roomId, LocalDate checkin, LocalDate checkout) {
        return read(s -> s.createQuery("""
                select count(b) from Booking b
                where b.room.id = :roomId
                  and b.status in (:active)
                  and b.checkinDate < :checkout
                  and b.checkoutDate > :checkin
                """, Long.class)
                .setParameter("roomId", roomId)
                .setParameter("active", ACTIVE_STATUSES)
                .setParameter("checkin", checkin)
                .setParameter("checkout", checkout)
                .getSingleResult());
    }

    /** Giữ tên cũ cho các chỗ đang gọi (BookingServiceImpl.isAvailable). */
    public boolean existsOverlap(Long roomId, LocalDate checkin, LocalDate checkout) {
        return countOverlap(roomId, checkin, checkout) > 0;
    }

    /**
     * Bản chạy <b>trong một Session đang mở transaction</b> (cùng Session với booking vừa tạo/sửa).
     *
     * <p>Khác {@link #countOverlap}: lần này không mở session mới bằng {@link #read}, mà đọc
     * ngay trong Session caller cung cấp — nên nó <b>thấy cả những thay đổi chưa commit</b> (đã flush).
     * Dùng trong {@link com.vivu.booking.service.impl.BookingServiceImpl#create}.
     */
    public long countOverlapInSession(Session s, Long roomId, LocalDate checkin, LocalDate checkout) {
        return s.createQuery("""
                select count(b) from Booking b
                where b.room.id = :roomId
                  and b.status in (:active)
                  and b.checkinDate < :checkout
                  and b.checkoutDate > :checkin
                """, Long.class)
                .setParameter("roomId", roomId)
                .setParameter("active", ACTIVE_STATUSES)
                .setParameter("checkin", checkin)
                .setParameter("checkout", checkout)
                .getSingleResult();
    }

    /**
     * Bản cùng-session của {@link #countActiveOnDate}: đếm booking ACTIVE còn phủ ngày date
     * trong Session hiện tại, gồm cả dòng vừa sửa trạng thái trong chính transaction này.
     * Dùng khi trả lịch (cancel/expire) — không cần exclude id vì Booking của ta
     * vừa được đổi sang CANCELLED/EXPIRED nên tự loại khỏi {@code ACTIVE_STATUSES}.
     */
    public long countActiveOnDateInSession(Session s, Long roomId, LocalDate date) {
        return s.createQuery("""
                select count(b) from Booking b
                where b.room.id = :roomId
                  and b.status in (:active)
                  and b.checkinDate <= :date
                  and b.checkoutDate > :date
                """, Long.class)
                .setParameter("roomId", roomId)
                .setParameter("active", ACTIVE_STATUSES)
                .setParameter("date", date)
                .getSingleResult();
    }

    /**
     * Đếm booking đang chiếm phòng phủ một ngày cụ thể, bỏ qua một booking (thường là booking đang hủy).
     * Dùng để quyết định trả RoomCalendar về AVAILABLE: chỉ trả khi không còn booking nào khác giữ ngày đó.
     */
    public long countActiveOnDate(Long roomId, LocalDate date, Long excludeBookingId) {
        return read(s -> s.createQuery("""
                select count(b) from Booking b
                where b.room.id = :roomId
                  and b.status in (:active)
                  and (:excludeId is null or b.id <> :excludeId)
                  and b.checkinDate <= :date
                  and b.checkoutDate > :date
                """, Long.class)
                .setParameter("roomId", roomId)
                .setParameter("active", ACTIVE_STATUSES)
                .setParameter("excludeId", excludeBookingId)
                .setParameter("date", date)
                .getSingleResult());
    }

    /**
     * Lấy các booking đang giữ chỗ đã hết hạn — HoldExpireScheduler quét mỗi phút.
     *
     * <h2>Vì sao có cả {@code PENDING_PAYMENT}?</h2>
     * <p>
     * Bản cũ chỉ lọc {@code status = HOLD}. Nhưng sau khi khách bấm "Thanh toán",
     * {@code PaymentServiceImpl} chuyển booking sang {@code PENDING_PAYMENT} — và từ đó
     * <b>không còn job nào đụng tới nó nữa</b>. Khách bỏ dở trang checkout là các đêm bị
     * BLOCKED vĩnh viễn: phòng trống mà không ai đặt được, không lỗi, không log. Đây là loại
     * bug "rò rỉ tồn kho" khó phát hiện nhất vì hệ thống vẫn chạy bình thường.
     *
     * <p>Cả hai trạng thái đều dùng chung một mốc {@code holdExpiresAt} (đếm ngược vẫn chạy
     * khi khách đang ở trang cổng thanh toán), nên gộp vào một truy vấn là đúng.
     *
     * <p>Chỉ select <b>id</b>, không fetch entity: scheduler mở session riêng cho từng booking
     * để expire (1 lỗi không kéo theo cả batch), nên trả entity đã bị detach ở đây chỉ tổ gây
     * LazyInitializationException khi đọc {@code getRoom()} ngoài session.
     *
     * @param now   mốc "bây giờ" — mọi giữ chỗ có holdExpiresAt &lt; now là quá hạn
     * @param limit số bản ghi tối đa một lượt quét (tránh quét cả bảng khi backlog lớn)
     */
    public List<Long> findExpiredHoldIds(LocalDateTime now, int limit) {
        return read(s -> s.createQuery("""
                select b.id from Booking b
                where b.status in (:holding) and b.holdExpiresAt is not null and b.holdExpiresAt < :now
                order by b.holdExpiresAt asc
                """, Long.class)
                .setParameter("holding", HOLDING_STATUSES)
                .setParameter("now", now)
                .setMaxResults(limit)
                .getResultList());
    }

    /**
     * Khoá booking bằng {@code SELECT ... FOR UPDATE} — <b>điểm vào duy nhất</b> cho mọi luồng
     * cần đổi trạng thái booking (create/cancel/confirm/expire/thanh toán).
     *
     * <p>Đặt ở DAO thay vì mỗi Service tự viết HQL vì thứ tự khoá là cam kết toàn hệ thống
     * ({@code Room → RoomCalendar → Booking → Payment}): hai luồng khoá ngược nhau là deadlock.
     * Gom về một chỗ thì không ai vô tình đổi thứ tự.
     *
     * <p>{@code left join fetch} room/user để sau khi Session đóng vẫn đọc được
     * {@code getRoom().getId()} / {@code getUser().getId()} mà không LazyInitializationException.
     */
    public static com.vivu.booking.entity.Booking lockBooking(Session s, Long bookingId) {
        com.vivu.booking.entity.Booking b = s.createQuery("""
                        select distinct b from Booking b
                        left join fetch b.room
                        left join fetch b.user
                        where b.id = :id
                        """, com.vivu.booking.entity.Booking.class)
                .setParameter("id", bookingId)
                .setLockMode(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
                .uniqueResult();
        if (b == null) {
            throw new com.vivu.booking.exception.ResourceNotFoundException("Booking not found: " + bookingId);
        }
        return b;
    }
}
