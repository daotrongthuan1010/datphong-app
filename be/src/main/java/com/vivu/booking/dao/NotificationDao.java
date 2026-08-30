package com.vivu.booking.dao;

import com.vivu.booking.entity.Notification;
import jakarta.persistence.EntityManager;
import java.util.List;

public class NotificationDao extends BaseDao<Notification, Long> {

    public NotificationDao() {
        super(Notification.class);
    }

    /** idx_notifications_user_unread (user_id, is_read, created_at) - danh sách thông báo, mới nhất trước */
    public List<Notification> findByUser(Long userId, Boolean isRead, int page, int size,
                                         EntityManager em) {
        StringBuilder jpql = new StringBuilder(
                "SELECT n FROM Notification n WHERE n.user.id = :userId");
        if (isRead != null) jpql.append(" AND n.isRead = :isRead");
        jpql.append(" ORDER BY n.createdAt DESC");

        var query = em.createQuery(jpql.toString(), Notification.class)
                .setParameter("userId", userId);
        if (isRead != null) query.setParameter("isRead", isRead);

        return query.setFirstResult(page * size).setMaxResults(size).getResultList();
    }

    /** idx_notifications_type (user_id, notif_type) */
    public List<Notification> findByUserAndType(Long userId, String notifType, EntityManager em) {
        return em.createQuery(
                        "SELECT n FROM Notification n WHERE n.user.id = :userId AND n.notifType = :type " +
                                "ORDER BY n.createdAt DESC", Notification.class)
                .setParameter("userId", userId)
                .setParameter("type", notifType)
                .getResultList();
    }

    public long countUnread(Long userId, EntityManager em) {
        return em.createQuery(
                        "SELECT COUNT(n) FROM Notification n WHERE n.user.id = :userId AND n.isRead = false",
                        Long.class)
                .setParameter("userId", userId)
                .getSingleResult();
    }

    /** Bulk update đánh dấu tất cả đã đọc - nhanh hơn nhiều so với load từng bản ghi rồi save lại. */
    public int markAllRead(Long userId, EntityManager em) {
        return em.createQuery(
                        "UPDATE Notification n SET n.isRead = true WHERE n.user.id = :userId AND n.isRead = false")
                .setParameter("userId", userId)
                .executeUpdate();
    }
}
