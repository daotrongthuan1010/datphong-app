package com.vivu.booking.service.impl;

import com.vivu.booking.common.PageResponse;
import com.vivu.booking.dao.ConversationDao;
import com.vivu.booking.dao.MessageDao;
import com.vivu.booking.dto.response.MessageResponse;
import com.vivu.booking.entity.Conversation;
import com.vivu.booking.entity.Message;
import com.vivu.booking.entity.User;
import com.vivu.booking.enums.MessageTypeEnum;
import com.vivu.booking.exception.BusinessException;
import com.vivu.booking.exception.ResourceNotFoundException;
import com.vivu.booking.mapper.MessageMapper;
import com.vivu.booking.service.MessageService;
import org.hibernate.Session;

import java.util.List;

import static com.vivu.booking.config.HibernateConfig.getSessionFactory;

public class MessageServiceImpl implements MessageService {

    private final MessageDao messageDao;
    private final ConversationDao conversationDao;

    public MessageServiceImpl(MessageDao messageDao, ConversationDao conversationDao) {
        this.messageDao = messageDao;
        this.conversationDao = conversationDao;
    }

    public MessageServiceImpl() {
        this(new MessageDao(), new ConversationDao());
    }

    private Conversation requireConversation(Long conversationId, Session session) {
        Conversation c = session.find(Conversation.class, conversationId);
        if (c == null) throw new ResourceNotFoundException("Không tìm thấy hội thoại: " + conversationId);
        return c;
    }

    private void assertParticipant(Long userId, Conversation c) {
        if (!c.getUser().getId().equals(userId) && !c.getHost().getId().equals(userId)) {
            throw new BusinessException(403, "Không có quyền thao tác trên hội thoại này");
        }
    }

    @Override
    public PageResponse<MessageResponse> listMessages(Long userId, Long conversationId, int page, int size) {
        try (Session session = getSessionFactory().openSession()) {
            Conversation c = requireConversation(conversationId, session);
            assertParticipant(userId, c);
            long total = messageDao.countByConversation(conversationId, session);
            List<Message> rows = messageDao.findByConversationId(conversationId, page, size, session);
            List<MessageResponse> content = rows.stream().map(MessageMapper::toResponse).toList();
            return PageResponse.of(content, page, size, total);
        }
    }

    @Override
    public MessageResponse sendMessage(Long senderId, Long conversationId, String content) {
        if (content == null || content.isBlank()) throw new BusinessException(400, "Nội dung tin nhắn không được để trống");
        if (content.length() > 4000) throw new BusinessException(400, "Tin nhắn tối đa 4000 ký tự");

        try (Session session = getSessionFactory().openSession()) {
            var tx = session.beginTransaction();
            Message saved;
            try {
                Conversation c = requireConversation(conversationId, session);
                assertParticipant(senderId, c);
                User sender = session.find(User.class, senderId);
                if (sender == null) throw new ResourceNotFoundException("Không tìm thấy tài khoản gửi");
                Message m = Message.builder()
                        .conversation(c)
                        .sender(sender)
                        .msgType(MessageTypeEnum.TEXT)
                        .content(content.trim())
                        .build();
                session.persist(m);
                tx.commit();
                saved = m;
            } catch (RuntimeException e) {
                if (tx.isActive()) tx.rollback();
                throw e;
            }
            return MessageMapper.toResponse(saved);
        }
    }

    @Override
    public void markRead(Long readerId, Long conversationId) {
        try (Session session = getSessionFactory().openSession()) {
            var tx = session.beginTransaction();
            try {
                Conversation c = requireConversation(conversationId, session);
                assertParticipant(readerId, c);
                messageDao.markRead(conversationId, readerId, session);
                tx.commit();
            } catch (RuntimeException e) {
                if (tx.isActive()) tx.rollback();
                throw e;
            }
        }
    }

    @Override
    public long countUnread(Long userId, Long conversationId) {
        try (Session session = getSessionFactory().openSession()) {
            Conversation c = requireConversation(conversationId, session);
            assertParticipant(userId, c);
            return messageDao.countUnread(conversationId, userId, session);
        }
    }
}
