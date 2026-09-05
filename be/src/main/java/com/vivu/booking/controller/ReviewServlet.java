package com.vivu.booking.controller;

import com.vivu.booking.dto.request.ReviewCreateRequest;
import com.vivu.booking.dto.response.AuthTokenResponse;
import com.vivu.booking.dto.response.UsersLoginResponse;
import com.vivu.booking.service.ReviewService;
import com.vivu.booking.service.impl.ReviewServiceImpl;
import com.vivu.booking.utils.JsonUtils;
import com.vivu.booking.utils.ServletUtils;
import com.vivu.booking.utils.ValidationUtils;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Routes:
 *   GET  /api/reviews?roomId=&page=&size=      -> review đang hiển thị + điểm trung bình (public)
 *   POST /api/reviews                          -> tạo review (multipart: review JSON + media ảnh/video)
 */
@WebServlet(urlPatterns = "/api/reviews/*")
@MultipartConfig(fileSizeThreshold = 1024 * 1024, maxFileSize = 60 * 1024 * 1024, maxRequestSize = 150 * 1024 * 1024)
public class ReviewServlet extends HttpServlet {

    private ReviewService reviewService;

    @Override
    public void init() {
        this.reviewService = new ReviewServiceImpl();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String roomIdRaw = req.getParameter("roomId");
            if (roomIdRaw == null || roomIdRaw.isBlank()) {
                throw new com.vivu.booking.exception.BusinessException(400, "Thiếu tham số roomId");
            }
            long roomId = Long.parseLong(roomIdRaw.trim());
            int page = ServletUtils.parseIntParam(req, "page", 0);
            int size = Math.min(ServletUtils.parseIntParam(req, "size", 10), 50);
            if (page < 0) page = 0;
            ServletUtils.ok(req, resp, reviewService.listByRoom(roomId, page, size));
        } catch (Exception e) {
            ServletUtils.handleException(req, resp, e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            Long userId = currentUserId(req);

            String reviewJson = req.getParameter("review");
            if (reviewJson == null || reviewJson.isBlank()) {
                throw new com.vivu.booking.exception.BusinessException(400, "Thiếu trường 'review' (JSON: bookingId, rating, comment)");
            }
            ReviewCreateRequest body = JsonUtils.fromJson(reviewJson, ReviewCreateRequest.class);
            ValidationUtils.validate(body);

            List<Part> mediaParts = new ArrayList<>();
            for (Part part : req.getParts()) {
                String name = part.getName();
                if (part.getSize() > 0 && ("files".equals(name) || "file".equals(name) || name.startsWith("media"))) {
                    mediaParts.add(part);
                }
            }

            var created = reviewService.create(userId, body, mediaParts);
            ServletUtils.created(req, resp, created);
        } catch (Exception e) {
            ServletUtils.handleException(req, resp, e);
        }
    }

    /** userId từ session (AuthServlet set khi /api/auth/login). */
    private static Long currentUserId(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session != null) {
            Object userAttr = session.getAttribute("user");
            if (userAttr instanceof AuthTokenResponse.UserSummary us && us.getId() != null) return us.getId();
            if (userAttr instanceof UsersLoginResponse ul && ul.getId() != null) return ul.getId();
        }
        throw new com.vivu.booking.exception.BusinessException(401, "Bạn chưa đăng nhập");
    }
}
