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

    /** Tất cả conversation của 1 participant (khách hoặc host) — dùng cho "Tin nhắn của tôi". */
    public List<Conversation> findByParticipant(Long userId, EntityManager em) {
        return em.createQuery(
                        "SELECT c FROM Conversation c WHERE c.user.id = :uid OR c.host.id = :uid ORDER BY c.createdAt DESC",
                        Conversation.class)
                .setParameter("uid", userId)
                .getResultList();
    }

    public List<Conversation> findByParticipant(Long userId, int page, int size, EntityManager em) {
        return em.createQuery(
                        "SELECT c FROM Conversation c WHERE c.user.id = :uid OR c.host.id = :uid ORDER BY c.createdAt DESC",
                        Conversation.class)
                .setParameter("uid", userId)
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList();
    }

    public long countByParticipant(Long userId, EntityManager em) {
        return em.createQuery(
                        "SELECT COUNT(c) FROM Conversation c WHERE c.user.id = :uid OR c.host.id = :uid", Long.class)
                .setParameter("uid", userId)
                .getSingleResult();
    }

    /** Tránh tạo trùng hội thoại: kiểm tra đã tồn tại giữa 1 user-host-room cụ thể chưa. */
    public Optional<Conversation> findExisting(Long userId, Long hostId, Long roomId, EntityManager em) {
        if (roomId != null) {
            List<Conversation> result = em.createQuery(
                            "SELECT c FROM Conversation c WHERE c.user.id = :userId AND c.host.id = :hostId " +
                                    "AND c.room.id = :roomId", Conversation.class)
                    .setParameter("userId", userId)
                    .setParameter("hostId", hostId)
                    .setParameter("roomId", roomId)
                    .getResultList();
            return result.stream().findFirst();
        }
        List<Conversation> result = em.createQuery(
                        "SELECT c FROM Conversation c WHERE c.user.id = :userId AND c.host.id = :hostId " +
                                "AND c.room IS NULL", Conversation.class)
                .setParameter("userId", userId)
                .setParameter("hostId", hostId)
                .getResultList();
        return result.stream().findFirst();
    }
}
