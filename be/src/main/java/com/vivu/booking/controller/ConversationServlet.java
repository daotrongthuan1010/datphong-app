package com.vivu.booking.controller;

import com.vivu.booking.dto.request.ConversationCreateRequest;
import com.vivu.booking.dto.request.MessageSendRequest;
import com.vivu.booking.exception.BusinessException;
import com.vivu.booking.service.ConversationService;
import com.vivu.booking.service.MessageService;
import com.vivu.booking.service.impl.ConversationServiceImpl;
import com.vivu.booking.service.impl.MessageServiceImpl;
import com.vivu.booking.utils.JwtUtil;
import com.vivu.booking.utils.ServletUtils;
import com.vivu.booking.utils.ValidationUtils;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Map;

/**
 * Hội thoại + tin nhắn khách ↔ host.
 *
 *   POST /api/conversations                  body { hostId, roomId? } → tạo/lấy hội thoại
 *   GET  /api/conversations                → danh sách của tôi
 *   GET  /api/conversations/{id}           → chi tiết
 *   GET  /api/conversations/{id}/messages  → tin nhắn (page, size)
 *   POST /api/conversations/{id}/messages  body { content } → gửi tin
 *   POST /api/conversations/{id}/read      → đánh dấu đã đọc
 */
@WebServlet(urlPatterns = "/api/conversations/*")
public class ConversationServlet extends HttpServlet {

    private ConversationService conversationService;
    private MessageService messageService;

    @Override
    public void init() {
        this.conversationService = new ConversationServiceImpl();
        this.messageService = new MessageServiceImpl();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String path = req.getPathInfo();
            Long uid = requireAuth(req);
            if (path == null || path.equals("/")) {
                int page = ServletUtils.parseIntParam(req, "page", 0);
                int size = Math.min(ServletUtils.parseIntParam(req, "size", 20), 100);
                if (page < 0) page = 0;
                if (size <= 0) size = 20;
                var result = conversationService.listMyConversations(uid, page, size);
                ServletUtils.ok(req, resp, result);
                return;
            }
            if (path.endsWith("/messages")) {
                Long cid = parseConversationIdFromPath(path);
                int page = ServletUtils.parseIntParam(req, "page", 0);
                int size = Math.min(ServletUtils.parseIntParam(req, "size", 50), 100);
                if (page < 0) page = 0;
                if (size <= 0) size = 20;
                var result = messageService.listMessages(uid, cid, page, size);
                try { messageService.markRead(uid, cid); } catch (Exception ignored) {}
                ServletUtils.ok(req, resp, result);
                return;
            }
            Long id = parseId(path);
            ServletUtils.ok(req, resp, conversationService.getById(uid, id));
        } catch (Exception e) {
            ServletUtils.handleException(req, resp, e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            Long uid = requireAuth(req);
            String path = req.getPathInfo();

            if (path != null && path.endsWith("/read")) {
                Long cid = parseConversationIdFromPath(path.substring(0, path.length() - "/read".length()));
                messageService.markRead(uid, cid);
                ServletUtils.ok(req, resp, Map.of("markedRead", true));
                return;
            }
            if (path != null && path.endsWith("/messages")) {
                Long cid = parseConversationIdFromPath(path);
                MessageSendRequest body;
                try {
                    body = ServletUtils.readBody(req, MessageSendRequest.class);
                } catch (Exception ex) {
                    throw new BusinessException(400, "Thân tin nhắn không hợp lệ: " + ex.getMessage());
                }
                if (body == null || body.getContent() == null || body.getContent().isBlank()) {
                    throw new BusinessException(400, "Nội dung tin nhắn không được để trống");
                }
                ValidationUtils.validate(body);
                if (body.getConversationId() != null && !cid.equals(body.getConversationId())) {
                    throw new BusinessException(400, "conversationId trong thân không khớp URL");
                }
                var created = messageService.sendMessage(uid, cid, body.getContent().trim());
                ServletUtils.created(req, resp, created);
                return;
            }
            if (path != null && !path.equals("/") && !path.isBlank()) {
                throw new BusinessException(404, "Không tìm thấy endpoint POST: /api/conversations" + path);
            }
            ConversationCreateRequest body = ServletUtils.readBody(req, ConversationCreateRequest.class);
            ValidationUtils.validate(body);
            var created = conversationService.getOrCreate(uid, body.getHostId(), body.getRoomId());
            ServletUtils.created(req, resp, created);
        } catch (Exception e) {
            ServletUtils.handleException(req, resp, e);
        }
    }

    private static Long parseId(String pathInfo) {
        if (pathInfo == null || pathInfo.equals("/"))
            throw new BusinessException(400, "Missing id in path");
        String s = pathInfo.replaceFirst("^/", "").split("/")[0];
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            throw new BusinessException(400, "Invalid id: " + s);
        }
    }

    private static Long parseConversationIdFromPath(String path) {
        // path dạng /{id}/messages hoặc /{id} — lấy phần số đầu
        String trimmed = path == null ? "" : path.trim();
        if (trimmed.startsWith("/")) trimmed = trimmed.substring(1);
        String first = trimmed.split("/")[0];
        try {
            return Long.parseLong(first);
        } catch (NumberFormatException e) {
            throw new BusinessException(400, "Invalid conversation id: " + first);
        }
    }

    private static Long requireAuth(HttpServletRequest req) {
        String header = req.getHeader("Authorization");
        if (header != null && header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            JwtUtil.Claims claims = JwtUtil.parse(header.substring(7).trim());
            if (!"access".equals(claims.getType())) throw new BusinessException(401, "Token không phải access token");
            return claims.getUserId();
        }
        HttpSession sess = req.getSession(false);
        if (sess != null) {
            Object userAttr = sess.getAttribute("user");
            if (userAttr instanceof com.vivu.booking.dto.response.AuthTokenResponse.UserSummary us && us.getId() != null) return us.getId();
            if (userAttr instanceof com.vivu.booking.dto.response.UsersLoginResponse ul && ul.getId() != null) return ul.getId();
        }
        throw new BusinessException(401, "Chưa đăng nhập");
    }
}
