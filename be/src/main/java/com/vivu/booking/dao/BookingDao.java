package com.vivu.booking.dao;

import com.vivu.booking.entity.Booking;
import com.vivu.booking.entity.Room;
import com.vivu.booking.enums.BookingStatusType;
import jakarta.persistence.EntityManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class BookingDao extends BaseDao<Booking,Long> {
    public BookingDao(){ super(Booking.class);}

    public Optional<Booking> findByBookingCode(String code, EntityManager em) {
        List<Booking> result = em.createQuery(
                        "SELECT b FROM Booking b WHERE b.bookingCode = :code", Booking.class)
                .setParameter("code", code)
                .getResultList();
        return result.stream().findFirst();
    }
    public List<Booking> findByUserId(Long userId, int page, int size, EntityManager em) {
        return em.createQuery(
                        "SELECT b FROM Booking b WHERE b.user.id = :userId ORDER BY b.createdAt DESC",
                        Booking.class)
                .setParameter("userId", userId)
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList();
    }

}
