package com.vivu.booking.dao;

import com.vivu.booking.entity.DynamicPricingSuggestion;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;

public class DynamicPricingSuggestionDao extends BaseDao<DynamicPricingSuggestion, Long> {

    public DynamicPricingSuggestionDao() {
        super(DynamicPricingSuggestion.class);
    }

    /** idx_dynamic_pricing_pending (room_id, target_date) WHERE applied=false */
    public List<DynamicPricingSuggestion> findPendingByRoom(Long roomId, EntityManager em) {
        return em.createQuery(
                        "SELECT d FROM DynamicPricingSuggestion d WHERE d.room.id = :roomId " +
                                "AND d.applied = false ORDER BY d.targetDate ASC", DynamicPricingSuggestion.class)
                .setParameter("roomId", roomId)
                .getResultList();
    }

    /** idx_dynamic_pricing_room_date */
    public DynamicPricingSuggestion findByRoomAndDate(Long roomId, LocalDate date, EntityManager em) {
        List<DynamicPricingSuggestion> result = em.createQuery(
                        "SELECT d FROM DynamicPricingSuggestion d WHERE d.room.id = :roomId " +
                                "AND d.targetDate = :date", DynamicPricingSuggestion.class)
                .setParameter("roomId", roomId)
                .setParameter("date", date)
                .getResultList();
        return result.stream().findFirst().orElse(null);
    }

    /** Đánh dấu đã áp dụng đề xuất giá (bulk update, không cần load Entity về). */
    public void markApplied(Long id, EntityManager em) {
        em.createQuery("UPDATE DynamicPricingSuggestion d SET d.applied = true WHERE d.id = :id")
                .setParameter("id", id)
                .executeUpdate();
    }
}
