package com.vivu.booking.dao;

import com.vivu.booking.entity.PointHistory;

import java.util.List;

public class PointHistoryDao extends BaseDao<PointHistory, Long> {

    public PointHistoryDao() {
        super(PointHistory.class);
    }

    /** idx_point_history_user_id (user_id, created_at) - lịch sử tích/trừ điểm mới nhất trước */
    public List<PointHistory> findByUserId(Long userId, int page, int size) {
        return read(s -> s.createQuery(
                        "SELECT p FROM PointHistory p LEFT JOIN FETCH p.booking WHERE p.user.id = :userId ORDER BY p.createdAt DESC",
                        PointHistory.class)
                .setParameter("userId", userId)
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList());
    }

    public long countByUserId(Long userId) {
        return read(s -> s.createQuery(
                        "SELECT COUNT(p) FROM PointHistory p WHERE p.user.id = :userId", Long.class)
                .setParameter("userId", userId)
                .getSingleResult());
    }

    public long sumPointsByUserId(Long userId) {
        Long sum = read(s -> s.createQuery(
                        "SELECT COALESCE(SUM(p.pointsChange), 0) FROM PointHistory p WHERE p.user.id = :userId", Long.class)
                .setParameter("userId", userId)
                .getSingleResult());
        return sum != null ? sum : 0L;
    }
}
