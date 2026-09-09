package com.vivu.booking.dao;

import com.vivu.booking.entity.Payment;
import com.vivu.booking.entity.PaymentAttempt;
import com.vivu.booking.enums.PaymentStatusType;
import jakarta.persistence.LockModeType;
import org.hibernate.Session;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/**
 * Các thao tác giao dịch tách ra để caller khống chế phạm vi transaction — service
 * cần khoá {@link Payment} và {@link com.vivu.booking.entity.Booking} trong <b>cùng một
 * Hibernate Session</b> thì mới giữ được thứ tự khoá ổn định (Room → RoomCalendar → Booking → Payment).
 */
public class PaymentDao extends BaseDao<Payment, Long> {

    public PaymentDao() { super(Payment.class); }

    /** Chạy một callback trong transaction của Payment (mở Session + khoá nếu cần). */
    public <T> T inTransaction(Function<Session, T> work) { return tx(work); }

    /**
     * Khoá booking bằng {@code FOR UPDATE} kèm room/user để tránh race
     * confirm/expire/cancel thanh toán cùng lúc.
     */
    public static com.vivu.booking.entity.Booking lockBooking(Session s, Long bookingId) {
        com.vivu.booking.entity.Booking b = s.createQuery("""
                        select distinct b from com.vivu.booking.entity.Booking b
                        left join fetch b.room
                        left join fetch b.user
                        where b.id = :id
                        """, com.vivu.booking.entity.Booking.class)
                .setParameter("id", bookingId)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .uniqueResult();
        if (b == null) throw new com.vivu.booking.exception.ResourceNotFoundException("Booking not found: " + bookingId);
        return b;
    }

    /**
     * Tạo bản ghi thanh toán cho booking — idempotent theo {@code booking_id}.
     *
     * <p>Dùng {@code INSERT ... ON CONFLICT DO NOTHING} style: thử insert, nếu đã tồn tại
     * {@code booking_id} (unique) thì lock và trả về payment hiện có. Tránh dùng
     * lock-for-share trên payment trước vì payment chưa tồn tại.
     */
    public Payment createForBookingIfAbsent(Session s, Long bookingId,
                                            com.vivu.booking.enums.PaymentMethodType method,
                                            BigDecimal amount, String currency) {
        com.vivu.booking.entity.Booking booking = s.find(com.vivu.booking.entity.Booking.class, bookingId);
        if (booking == null) throw new com.vivu.booking.exception.ResourceNotFoundException("Booking not found: " + bookingId);

        Payment existing = s.createQuery("""
                        select p from Payment p where p.booking.id = :bid
                        """, Payment.class)
                .setParameter("bid", bookingId)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .uniqueResultOptional().orElse(null);
        if (existing != null) return existing;

        Payment payment = Payment.builder()
                .booking(booking)
                .method(method)
                .amount(amount)
                .currency(currency == null ? "VND" : currency)
                .status(PaymentStatusType.PENDING)
                .gatewayTransactionRef("pending-" + UUID.randomUUID().toString().substring(0, 8))
                .build();
        s.persist(payment);
        s.flush(); // sinh id ngay để PaymentAttempt tham chiếu
        return payment;
    }

    /** Payment theo bookingId — join fetch booking/room/user cho owner check sau khi session đóng. */
    public Optional<Payment> findByBookingId(Long bookingId) {
        return read(s -> s.createQuery("""
                        select distinct p from Payment p
                        join fetch p.booking b
                        join fetch b.user
                        join fetch b.room
                        where b.id = :bookingId
                        """, Payment.class)
                .setParameter("bookingId", bookingId)
                .uniqueResultOptional());
    }

    /**
     * Tìm Payment theo {@code gateway_transaction_ref}.
     *
     * <p>Ưu tiên tìm qua {@link PaymentAttempt} để chấp nhận <b>mọi mã từng phát hành</b>
     * (nhiều attempt của cùng 1 payment). Fallback sang {@code payments.gateway_transaction_ref}
     * cho dữ liệu cũ chưa có attempt.
     */
    public Optional<Payment> findByGatewayRef(String gatewayRef) {
        return read(s -> {
            Long pid = s.createQuery("""
                            select a.payment.id from PaymentAttempt a
                            where a.gatewayTransactionRef = :ref
                            """, Long.class)
                    .setParameter("ref", gatewayRef)
                    .setMaxResults(1).uniqueResultOptional().orElse(null);
            if (pid != null) {
                return s.createQuery("""
                                select distinct p from Payment p
                                join fetch p.booking b
                                join fetch b.user
                                join fetch b.room
                                where p.id = :pid
                                """, Payment.class).setParameter("pid", pid).uniqueResultOptional();
            }
            return s.createQuery("""
                            select distinct p from Payment p
                            join fetch p.booking b
                            join fetch b.user
                            join fetch b.room
                            where p.gatewayTransactionRef = :ref
                            """, Payment.class).setParameter("ref", gatewayRef).uniqueResultOptional();
        });
    }

    /**
     * Khoá Payment theo {@code gateway_transaction_ref} trong Session đang mở (dùng cho webhook).
     *
     * <p>Khoá bằng {@code FOR UPDATE} qua lookup {@link PaymentAttempt} + lock Payment thực.
     */
    public Optional<Payment> findByGatewayRefForUpdate(Session s, String gatewayRef) {
        Long pid = s.createQuery("""
                        select a.payment.id from PaymentAttempt a
                        where a.gatewayTransactionRef = :ref
                        """, Long.class).setParameter("ref", gatewayRef).setMaxResults(1)
                .uniqueResultOptional().orElse(null);
        if (pid != null) {
            return s.createQuery("""
                            select distinct p from Payment p
                            join fetch p.booking b
                            join fetch b.user
                            join fetch b.room
                            where p.id = :pid
                            """, Payment.class).setParameter("pid", pid)
                    .setLockMode(LockModeType.PESSIMISTIC_WRITE).uniqueResultOptional();
        }
        return s.createQuery("""
                        select distinct p from Payment p
                        join fetch p.booking b
                        join fetch b.user
                        join fetch b.room
                        where p.gatewayTransactionRef = :ref
                        """, Payment.class).setParameter("ref", gatewayRef)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE).uniqueResultOptional();
    }

    /** Khoá Payment theo bookingId trong Session đang mở — dùng ở createPayment. */
    public Optional<Payment> findByBookingIdForUpdate(Session s, Long bookingId) {
        return s.createQuery("""
                        select distinct p from Payment p
                        join fetch p.booking b
                        join fetch b.user
                        join fetch b.room
                        where b.id = :bookingId
                        """, Payment.class)
                .setParameter("bookingId", bookingId)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .uniqueResultOptional();
    }

    /**
     * Ghi một {@link PaymentAttempt} ứng với một lần cổng cấp mã mới.
     * Idempotent theo {@code gatewayTransactionRef} — ref trùng thì trả về bản ghi cũ.
     */
    public PaymentAttempt recordAttempt(Session s, Payment payment, String gatewayRef,
                                        BigDecimal amount, String currency) {
        PaymentAttempt existing = s.createQuery("""
                        select a from PaymentAttempt a where a.gatewayTransactionRef = :ref
                        """, PaymentAttempt.class).setParameter("ref", gatewayRef)
                .uniqueResultOptional().orElse(null);
        if (existing != null) return existing;
        PaymentAttempt a = PaymentAttempt.builder()
                .payment(payment)
                .gatewayTransactionRef(gatewayRef)
                .amount(amount)
                .currency(currency == null ? "VND" : currency)
                .build();
        s.persist(a);
        return a;
    }

    /** Payment + booking + user + room theo id — màn chi tiết / owner check. */
    public Optional<Payment> findWithBookingById(Long id) {
        return read(s -> s.createQuery("""
                        select distinct p from Payment p
                        join fetch p.booking b
                        join fetch b.user
                        join fetch b.room
                        where p.id = :id
                        """, Payment.class).setParameter("id", id).uniqueResultOptional());
    }
    /** Dung trong Session dang mo — true neu booking da co Payment SUCCESS (khong can lock). */
    public boolean existsSuccessForBookingInSession(org.hibernate.Session s, Long bookingId) {
        Long c = s.createQuery("""
                        select count(p) from Payment p
                        where p.booking.id = :bid and p.status = :ok
                        """, Long.class)
                .setParameter("bid", bookingId)
                .setParameter("ok", PaymentStatusType.SUCCESS)
                .getSingleResult();
        return c != null && c > 0;
    }


}
