package com.vivu.booking.dao;

import com.vivu.booking.entity.Conversation;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;

public class ConversationDao extends BaseDao<Conversation, Long> {

    public ConversationDao() {
        super(Conversation.class);
    }

    /** idx_conversations_user_id */
    public List<Conversation> findByUserId(Long userId, EntityManager em) {
        return em.createQuery(
                        "SELECT c FROM Conversation c WHERE c.user.id = :userId ORDER BY c.createdAt DESC",
                        Conversation.class)
                .setParameter("userId", userId)
                .getResultList();
    }

    /** idx_conversations_host_id */
    public List<Conversation> findByHostId(Long hostId, EntityManager em) {
        return em.createQuery(
                        "SELECT c FROM Conversation c WHERE c.host.id = :hostId ORDER BY c.createdAt DESC",
                        Conversation.class)
                .setParameter("hostId", hostId)
                .getResultList();
    }

    /** Tránh tạo trùng hội thoại: kiểm tra đã tồn tại giữa 1 user-host-room cụ thể chưa. */
    public Optional<Conversation> findExisting(Long userId, Long hostId, Long roomId, EntityManager em) {
        List<Conversation> result = em.createQuery(
                        "SELECT c FROM Conversation c WHERE c.user.id = :userId AND c.host.id = :hostId " +
                                "AND c.room.id = :roomId", Conversation.class)
                .setParameter("userId", userId)
                .setParameter("hostId", hostId)
                .setParameter("roomId", roomId)
                .getResultList();
        return result.stream().findFirst();
    }
}
