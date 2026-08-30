package com.vivu.booking.dao;

import com.vivu.booking.entity.AI_Recommendations;
import jakarta.persistence.EntityManager;
import java.util.List;

public class AI_RecommendationsDao extends BaseDao<AI_Recommendations, Long> {

    public AI_RecommendationsDao() {
        super(AI_Recommendations.class);
    }

    /** idx_ai_recommendations_user (user_id, score DESC) - top-N phòng gợi ý cho user */
    public List<AI_Recommendations> findTopByUser(Long userId, int limit, EntityManager em) {
        return em.createQuery(
                        "SELECT r FROM AI_Recommendations r WHERE r.user.id = :userId " +
                                "ORDER BY r.score DESC", AI_Recommendations.class)
                .setParameter("userId", userId)
                .setMaxResults(limit)
                .getResultList();
    }

    /** Xoá gợi ý cũ trước khi ghi batch gợi ý mới cho 1 user (tránh dữ liệu chồng chéo). */
    public int deleteAllByUser(Long userId, EntityManager em) {
        return em.createQuery("DELETE FROM AI_Recommendations r WHERE r.user.id = :userId")
                .setParameter("userId", userId)
                .executeUpdate();
    }
}
