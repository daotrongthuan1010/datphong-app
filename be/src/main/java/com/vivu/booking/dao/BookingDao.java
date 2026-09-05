package com.vivu.booking.dao;

import com.vivu.booking.entity.Booking;
import com.vivu.booking.enums.BookingStatusType;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class BookingDao extends BaseDao<Booking, Long> {
    public BookingDao() {
        super(Booking.class);
    }

    public Optional<Booking> findByBookingCode(String code) {
        return read(s -> s.createQuery("from Booking where bookingCode = :code", Booking.class)
                .setParameter("code", code).uniqueResultOptional());
    }

    /** Lay kem user/room/voucher de map response sau khi session dong (tranh LazyInitializationException). */
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
     * Kiem tra phong co bi trung lich trong khoang [checkin, checkout) khong.
     * Chi tinh cac booking chua bi huy.
     */
    public boolean existsOverlap(Long roomId, LocalDate checkin, LocalDate checkout) {
        return read(s -> s.createQuery("""
                select count(b) from Booking b
                where b.room.id = :roomId
                  and b.status <> :cancelled
                  and b.checkinDate < :checkout
                  and b.checkoutDate > :checkin
                """, Long.class)
                .setParameter("roomId", roomId)
                .setParameter("cancelled", BookingStatusType.CANCELLED)
                .setParameter("checkin", checkin)
                .setParameter("checkout", checkout)
                .getSingleResult() > 0);
    }
}
