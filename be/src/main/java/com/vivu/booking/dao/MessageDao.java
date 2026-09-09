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

    /** 1 tin mới nhất — dùng để set lastMessagePreview cho danh sách hội thoại. */
    public Message findLatest(Long conversationId, EntityManager em) {
        List<Message> rows = em.createQuery(
                        "SELECT m FROM Message m WHERE m.conversation.id = :conversationId ORDER BY m.createdAt DESC",
                        Message.class)
                .setParameter("conversationId", conversationId)
                .setMaxResults(1)
                .getResultList();
        return rows.isEmpty() ? null : rows.get(0);
    }

    public long countByConversation(Long conversationId, EntityManager em) {
        return em.createQuery(
                        "SELECT COUNT(m) FROM Message m WHERE m.conversation.id = :conversationId", Long.class)
                .setParameter("conversationId", conversationId)
                .getSingleResult();
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

    /** Đánh dấu đã đọc: các tin của người khác gửi trong hội thoại này. */
    public int markRead(Long conversationId, Long readerId, EntityManager em) {
        return em.createQuery(
                        "UPDATE Message m SET m.readAt = CURRENT_TIMESTAMP " +
                                "WHERE m.conversation.id = :cid AND m.sender.id <> :readerId AND m.readAt IS NULL")
                .setParameter("cid", conversationId)
                .setParameter("readerId", readerId)
                .executeUpdate();
    }
}
