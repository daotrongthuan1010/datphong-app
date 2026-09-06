package com.vivu.booking.dao;

import com.vivu.booking.entity.Payment;
import com.vivu.booking.enums.PaymentStatusType;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Optional;

public class PaymentDao extends BaseDao<Payment, Long> {

    public PaymentDao() {
        super(Payment.class);
    }
    // tránh trường hợp booking 2 lần lại thanh toán lần lượt
    public Optional<Payment> findByBookingId(Long bookingId) {
        return read(s -> s.createQuery("""
                        select p from Payment p where p.booking.id=:bookingId
                        """, Payment.class)
                .setParameter("bookingId", bookingId)
                .uniqueResultOptional());
    }
}
