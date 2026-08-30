package com.vivu.booking.dao;

import com.vivu.booking.entity.SupportTicket;
import com.vivu.booking.enums.TicketStatusType;
import jakarta.persistence.EntityManager;
import java.util.List;

public class SupportTicketDao extends BaseDao<SupportTicket, Long> {

    public SupportTicketDao() {
        super(SupportTicket.class);
    }

    /** idx_support_tickets_created_by */
    public List<SupportTicket> findByCreatedBy(Long userId, int page, int size, EntityManager em) {
        return em.createQuery(
                        "SELECT t FROM SupportTicket t WHERE t.createdBy.id = :userId " +
                                "ORDER BY t.createdAt DESC", SupportTicket.class)
                .setParameter("userId", userId)
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList();
    }

    /** idx_support_tickets_status - Admin xử lý ticket theo hàng đợi */
    public List<SupportTicket> findByStatus(TicketStatusType status, int page, int size,
                                            EntityManager em) {
        return em.createQuery(
                        "SELECT t FROM SupportTicket t WHERE t.status = :status ORDER BY t.createdAt ASC",
                        SupportTicket.class)
                .setParameter("status", status)
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList();
    }

    /** idx_support_tickets_against_user_id - đánh giá độ tin cậy của 1 user/host bị khiếu nại nhiều lần */
    public long countAgainstUser(Long userId, EntityManager em) {
        return em.createQuery(
                        "SELECT COUNT(t) FROM SupportTicket t WHERE t.againstUser.id = :userId", Long.class)
                .setParameter("userId", userId)
                .getSingleResult();
    }

    /** idx_support_tickets_booking_id */
    public List<SupportTicket> findByBookingId(Long bookingId, EntityManager em) {
        return em.createQuery(
                        "SELECT t FROM SupportTicket t WHERE t.booking.id = :bookingId", SupportTicket.class)
                .setParameter("bookingId", bookingId)
                .getResultList();
    }

    /** idx_support_tickets_resolved_by - báo cáo hiệu suất xử lý của từng nhân viên hỗ trợ */
    public List<SupportTicket> findByResolvedBy(Long adminId, int page, int size, EntityManager em) {
        return em.createQuery(
                        "SELECT t FROM SupportTicket t WHERE t.resolvedBy.id = :adminId " +
                                "ORDER BY t.resolvedAt DESC", SupportTicket.class)
                .setParameter("adminId", adminId)
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList();
    }
}
