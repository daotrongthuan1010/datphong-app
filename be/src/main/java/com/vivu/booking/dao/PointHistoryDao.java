package com.vivu.booking.dao;

import com.vivu.booking.entity.PointHistory;
import jakarta.persistence.EntityManager;
import java.util.List;

public class PointHistoryDao extends BaseDao<PointHistory, Long> {

    public PointHistoryDao() {
        super(PointHistory.class);
    }

    /** idx_point_history_user_id (user_id, created_at) - lịch sử tích/trừ điểm mới nhất trước */
    public List<PointHistory> findByUserId(Long userId, int page, int size, EntityManager em) {
        return em.createQuery(
                        "SELECT p FROM PointHistory p WHERE p.user.id = :userId ORDER BY p.createdAt DESC",
                        PointHistory.class)
                .setParameter("userId", userId)
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList();
    }
}
