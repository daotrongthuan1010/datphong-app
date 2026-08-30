package com.vivu.booking.dao;

import com.vivu.booking.entity.UserBehaviorLog;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;

public class UserBehaviorLogDao extends BaseDao<UserBehaviorLog, Long> {

    public UserBehaviorLogDao() {
        super(UserBehaviorLog.class);
    }

    /** idx_behavior_user_time (user_id, created_at) - dữ liệu đầu vào cho AI recommendation */
    public List<UserBehaviorLog> findByUserId(Long userId, LocalDateTime from, LocalDateTime to,
                                              EntityManager em) {
        return em.createQuery(
                        "SELECT b FROM UserBehaviorLog b WHERE b.user.id = :userId " +
                                "AND b.createdAt BETWEEN :from AND :to ORDER BY b.createdAt DESC",
                        UserBehaviorLog.class)
                .setParameter("userId", userId)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();
    }

    /**
     * idx_user_behavior_room_action (room_id, action_type) - gộp số lượt xem/click theo
     * phòng, dùng cho AI Dynamic Pricing đánh giá độ "hot" của phòng.
     */
    public long countByRoomAndAction(Long roomId, String actionType, EntityManager em) {
        return em.createQuery(
                        "SELECT COUNT(b) FROM UserBehaviorLog b WHERE b.room.id = :roomId " +
                                "AND b.actionType = :actionType", Long.class)
                .setParameter("roomId", roomId)
                .setParameter("actionType", actionType)
                .getSingleResult();
    }
}
