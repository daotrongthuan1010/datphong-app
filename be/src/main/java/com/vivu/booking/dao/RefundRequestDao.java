package com.vivu.booking.dao;

import com.vivu.booking.entity.RefundRequest;
import com.vivu.booking.enums.RefundStatusType;
import jakarta.persistence.EntityManager;
import java.util.List;

public class RefundRequestDao extends BaseDao<RefundRequest, Long> {

    public RefundRequestDao() {
        super(RefundRequest.class);
    }

    /** idx_refund_requests_booking_id */
    public List<RefundRequest> findByBookingId(Long bookingId, EntityManager em) {
        return em.createQuery(
                        "SELECT r FROM RefundRequest r WHERE r.booking.id = :bookingId " +
                                "ORDER BY r.createdAt DESC", RefundRequest.class)
                .setParameter("bookingId", bookingId)
                .getResultList();
    }

    /** idx_refund_requests_status - Admin xử lý yêu cầu hoàn tiền đang chờ */
    public List<RefundRequest> findByStatus(RefundStatusType status, int page, int size,
                                            EntityManager em) {
        return em.createQuery(
                        "SELECT r FROM RefundRequest r WHERE r.status = :status " +
                                "ORDER BY r.createdAt ASC", RefundRequest.class)
                .setParameter("status", status)
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList();
    }
}
