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

    /** idx_payments_booking_id */
    public List<Payment> findByBookingId(Long bookingId, EntityManager em) {
        return em.createQuery(
                        "SELECT p FROM Payment p WHERE p.booking.id = :bookingId ORDER BY p.paidAt DESC",
                        Payment.class)
                .setParameter("bookingId", bookingId)
                .getResultList();
    }

    /**
     * UNIQUE index idx_payments_gateway_ref - dùng để chống xử lý callback trùng lặp
     * (idempotency) khi cổng thanh toán gọi webhook nhiều lần cho cùng 1 giao dịch.
     */
    public Optional<Payment> findByGatewayRef(String gatewayRef, EntityManager em) {
        List<Payment> result = em.createQuery(
                        "SELECT p FROM Payment p WHERE p.gatewayTransactionRef = :ref", Payment.class)
                .setParameter("ref", gatewayRef)
                .getResultList();
        return result.stream().findFirst();
    }

    /** idx_payments_status - Admin lọc báo cáo theo trạng thái thanh toán */
    public List<Payment> findByStatus(PaymentStatusType status, int page, int size, EntityManager em) {
        return em.createQuery(
                        "SELECT p FROM Payment p WHERE p.status = :status ORDER BY p.paidAt DESC", Payment.class)
                .setParameter("status", status)
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList();
    }
}
