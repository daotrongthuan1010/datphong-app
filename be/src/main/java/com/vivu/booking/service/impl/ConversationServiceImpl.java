package com.vivu.booking.service.impl;

import com.vivu.booking.common.PageResponse;
import com.vivu.booking.dao.ConversationDao;
import com.vivu.booking.dao.MessageDao;
import com.vivu.booking.dao.RoomDao;
import com.vivu.booking.dao.UsersDao;
import com.vivu.booking.dto.response.ConversationResponse;
import com.vivu.booking.entity.Conversation;
import com.vivu.booking.entity.Message;
import com.vivu.booking.entity.Room;
import com.vivu.booking.entity.User;
import com.vivu.booking.exception.BusinessException;
import com.vivu.booking.exception.ResourceNotFoundException;
import com.vivu.booking.mapper.ConversationMapper;
import com.vivu.booking.service.ConversationService;

import org.hibernate.Session;

import java.util.List;

import static com.vivu.booking.config.HibernateConfig.getSessionFactory;

public class ConversationServiceImpl implements ConversationService {

    private final ConversationDao conversationDao;
    private final MessageDao messageDao;
    private final UsersDao usersDao;
    private final RoomDao roomDao;

    public ConversationServiceImpl(ConversationDao conversationDao, MessageDao messageDao,
                                   UsersDao usersDao, RoomDao roomDao) {
        this.conversationDao = conversationDao;
        this.messageDao = messageDao;
        this.usersDao = usersDao;
        this.roomDao = roomDao;
    }

    public ConversationServiceImpl() {
        this(new ConversationDao(), new MessageDao(), new UsersDao(), new RoomDao());
    }

    @Override
    public ConversationResponse getOrCreate(Long actorId, Long hostId, Long roomId) {
        if (actorId == null) throw new BusinessException(401, "Chưa đăng nhập");
        if (hostId == null) throw new BusinessException(400, "Thiếu hostId");
        if (actorId.equals(hostId)) throw new BusinessException(400, "Không thể nhắn tin cho chính mình");

        try (Session session = getSessionFactory().openSession()) {
            var tx = session.beginTransaction();
            try {
                User actor = session.find(User.class, actorId);
                if (actor == null) throw new ResourceNotFoundException("Không tìm thấy người dùng: " + actorId);
                User host = session.find(User.class, hostId);
                if (host == null) throw new ResourceNotFoundException("Không tìm thấy host: " + hostId);
                Room room = null;
                if (roomId != null) {
                    room = session.find(Room.class, roomId);
                    if (room == null) throw new ResourceNotFoundException("Không tìm thấy phòng: " + roomId);
                }

                var existing = conversationDao.findExisting(actorId, hostId, roomId, session);
                if (existing.isPresent()) {
                    Conversation c = existing.get();
                    session.merge(c);
                    Message last = messageDao.findLatest(c.getId(), session);
                    tx.commit();
                    return ConversationMapper.toResponse(c, last);
                }
                // Thứ tự cố định: user = actor, host = host (không hoán đổi để findExisting/participant chính xác)
                Conversation conv = Conversation.builder()
                        .user(actor)
                        .host(host)
                        .room(room)
                        .build();
                session.persist(conv);
                tx.commit();
                return ConversationMapper.toResponse(conv, null);
            } catch (RuntimeException e) {
                if (tx.isActive()) tx.rollback();
                throw e;
            }
        }
    }

    @Override
    public PageResponse<ConversationResponse> listMyConversations(Long userId, int page, int size) {
        try (Session session = getSessionFactory().openSession()) {
            long total = conversationDao.countByParticipant(userId, session);
            List<Conversation> rows = conversationDao.findByParticipant(userId, page, size, session);
            List<ConversationResponse> content = rows.stream().map(c -> {
                Message last = messageDao.findLatest(c.getId(), session);
                return ConversationMapper.toResponse(c, last);
            }).toList();
            return PageResponse.of(content, page, size, total);
        }
    }

    @Override
    public ConversationResponse getById(Long userId, Long conversationId) {
        try (Session session = getSessionFactory().openSession()) {
            Conversation c = session.find(Conversation.class, conversationId);
            if (c == null) throw new ResourceNotFoundException("Không tìm thấy hội thoại: " + conversationId);
            if (!c.getUser().getId().equals(userId) && !c.getHost().getId().equals(userId)) {
                throw new BusinessException(403, "Không có quyền xem hội thoại này");
            }
            Message last = messageDao.findLatest(c.getId(), session);
            return ConversationMapper.toResponse(c, last);
        }
    }
}
