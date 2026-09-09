package com.vivu.booking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vivu.booking.dto.request.UserProfileUpdateRequest;
import com.vivu.booking.dto.response.AuthTokenResponse;
import com.vivu.booking.dto.response.UsersLoginResponse;
import com.vivu.booking.dto.response.UsersResponse;
import com.vivu.booking.exception.BusinessException;
import com.vivu.booking.service.UserService;
import com.vivu.booking.service.impl.UserServiceImpl;
import com.vivu.booking.dao.RoleDao;
import com.vivu.booking.utils.JwtUtil;
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

/**
 * Hồ sơ cá nhân của chính người đang đăng nhập — khác /api/users/* (admin, cần USER_WRITE).
 *
 *   GET /api/profile/me        - xem thông tin của mình
 *   PUT /api/profile/me        - sửa fullName/email/phone/gender (+ ảnh đại diện tùy chọn)
 *
 * PUT nhận 2 dạng: JSON thuần khi không đổi ảnh, hoặc multipart với field "profile"
 * chứa JSON + field "file" là ảnh (giống UserServlet để FE tái dùng cách gửi FormData).
 */
@WebServlet(urlPatterns = "/api/profile/*")
@MultipartConfig(fileSizeThreshold = 1024 * 1024, maxFileSize = 10 * 1024 * 1024, maxRequestSize = 20 * 1024 * 1024)
public class UserProfileServlet extends HttpServlet {

    private UserService userService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void init() {
        this.userService = new UserServiceImpl(new RoleDao());
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String path = req.getPathInfo();
            if (path == null || !path.startsWith("/me")) {
                throw new BusinessException(404, "Không tìm thấy endpoint: /api/profile" + (path == null ? "" : path));
            }
            Long uid = requireAuth(req);
            UsersResponse me = userService.getProfile(uid);
            ServletUtils.ok(req, resp, me);
        } catch (Exception e) {
            ServletUtils.handleException(req, resp, e);
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String path = req.getPathInfo();
            if (path == null || !path.startsWith("/me")) {
                throw new BusinessException(404, "Không tìm thấy endpoint: /api/profile" + (path == null ? "" : path));
            }
            Long uid = requireAuth(req);

            UserProfileUpdateRequest body;
            Part avatarPart = null;
            String contentType = req.getContentType();
            if (contentType != null && contentType.toLowerCase().startsWith("multipart/")) {
                String profileJson = req.getParameter("profile");
                if (profileJson == null || profileJson.isBlank()) {
                    throw new BusinessException(400, "Thiếu trường 'profile' (JSON: fullName, email, phone, gender)");
                }
                body = objectMapper.readValue(profileJson, UserProfileUpdateRequest.class);
                avatarPart = req.getPart("file");
            } else {
                body = ServletUtils.readBody(req, UserProfileUpdateRequest.class);
            }
            ValidationUtils.validate(body);

            UsersResponse updated = userService.updateProfile(uid, body, avatarPart);
            ServletUtils.ok(req, resp, updated);
        } catch (Exception e) {
            ServletUtils.handleException(req, resp, e);
        }
    }

    private static Long requireAuth(HttpServletRequest req) {
        String header = req.getHeader("Authorization");
        if (header != null && header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            JwtUtil.Claims claims = JwtUtil.parse(header.substring(7).trim());
            if (!"access".equals(claims.getType())) {
                throw new BusinessException(401, "Token không phải access token");
            }
            return claims.getUserId();
        }
        HttpSession sess = req.getSession(false);
        if (sess != null) {
            Object userAttr = sess.getAttribute("user");
            if (userAttr instanceof AuthTokenResponse.UserSummary us && us.getId() != null) return us.getId();
            if (userAttr instanceof UsersLoginResponse ul && ul.getId() != null) return ul.getId();
        }
        throw new BusinessException(401, "Chưa đăng nhập");
    }
}
