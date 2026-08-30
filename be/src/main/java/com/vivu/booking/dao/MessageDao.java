package com.vivu.booking.dao;

import com.vivu.booking.entity.Message;
import jakarta.persistence.EntityManager;
import java.util.List;

public class MessageDao extends BaseDao<Message, Long> {

    public MessageDao() {
        super(Message.class);
    }

    /** idx_messages_conversation (conversation_id, created_at) - phân trang tin nhắn mới nhất trước */
    public List<Message> findByConversationId(Long conversationId, int page, int size, EntityManager em) {
        return em.createQuery(
                        "SELECT m FROM Message m WHERE m.conversation.id = :conversationId " +
                                "ORDER BY m.createdAt DESC", Message.class)
                .setParameter("conversationId", conversationId)
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList();
    }

    /** idx_messages_sender_id - "tin nhắn tôi đã gửi" */
    public List<Message> findBySenderId(Long senderId, int page, int size, EntityManager em) {
        return em.createQuery(
                        "SELECT m FROM Message m WHERE m.sender.id = :senderId ORDER BY m.createdAt DESC",
                        Message.class)
                .setParameter("senderId", senderId)
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList();
    }

    /** Đếm tin nhắn chưa đọc trong 1 hội thoại (read_at IS NULL) để badge số trên UI chat. */
    public long countUnread(Long conversationId, Long excludeSenderId, EntityManager em) {
        return em.createQuery(
                        "SELECT COUNT(m) FROM Message m WHERE m.conversation.id = :conversationId " +
                                "AND m.readAt IS NULL AND m.sender.id <> :excludeSenderId", Long.class)
                .setParameter("conversationId", conversationId)
                .setParameter("excludeSenderId", excludeSenderId)
                .getSingleResult();
    }
}
