package com.vivu.booking.controller;

import com.vivu.booking.dto.request.*;
import com.vivu.booking.dto.response.AuthTokenResponse;
import com.vivu.booking.dto.response.UsersResponse;
import com.vivu.booking.service.AuthService;
import com.vivu.booking.service.impl.AuthServiceImpl;
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
 * Gom 8 API xác thực vào 1 Servlet, phân biệt qua pathInfo:
 *
 *   POST /api/auth/register          - Public
 *   POST /api/auth/otp/send          - Public
 *   POST /api/auth/otp/verify        - Public
 *   POST /api/auth/login             - Public
 *   POST /api/auth/refresh-token     - Public
 *   POST /api/auth/forgot-password   - Public
 *   POST /api/auth/reset-password    - Public
 *   POST /api/auth/logout            - User (yêu cầu đã đăng nhập)
 *
 * QUAN TRỌNG: AuthenFilter và AuthorFilter hiện chặn TẤT CẢ request trừ "/login"
 * - đã cập nhật whitelist trong 2 filter đó để các API "Public" ở trên đi qua
 * mà không cần session. Xem comment trong AuthenFilter.java / AuthorFilter.java.
 */
@WebServlet(urlPatterns = "/api/auth/*")
public class AuthServlet extends HttpServlet {

    private AuthService authService;

    @Override
    public void init() {
        this.authService = new AuthServiceImpl();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        try {
            if (path == null) {
                ServletUtils.writeJson(resp, 404, Map.of("success", false, "message", "Thiếu path con của /api/auth"));
                return;
            }
            switch (path) {
                case "/register" -> handleRegister(req, resp);
                case "/otp/send" -> handleSendOtp(req, resp);
                case "/otp/verify" -> handleVerifyOtp(req, resp);
                case "/login" -> handleLogin(req, resp);
                case "/refresh-token" -> handleRefreshToken(req, resp);
                case "/forgot-password" -> handleForgotPassword(req, resp);
                case "/reset-password" -> handleResetPassword(req, resp);
                case "/logout" -> handleLogout(req, resp);
                default -> ServletUtils.writeJson(resp, 404,
                        Map.of("success", false, "message", "Không tìm thấy endpoint: /api/auth" + path));
            }
        } catch (Exception e) {
            ServletUtils.handleException(req, resp, e);
        }
    }

    private void handleRegister(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        RegisterRequest body = ServletUtils.readBody(req, RegisterRequest.class);
        ValidationUtils.validate(body);
        UsersResponse created = authService.register(body);
        ServletUtils.created(req, resp, created);
    }

    private void handleSendOtp(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        SendOtpRequest body = ServletUtils.readBody(req, SendOtpRequest.class);
        ValidationUtils.validate(body);
        authService.sendOtp(body);
        ServletUtils.ok(req, resp, Map.of("message", "Đã gửi mã OTP, vui lòng kiểm tra email"));
    }

    private void handleVerifyOtp(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        OtpVerifyRequest body = ServletUtils.readBody(req, OtpVerifyRequest.class);
        ValidationUtils.validate(body);
        authService.verifyOtp(body);
        ServletUtils.ok(req, resp, Map.of("valid", true, "message", "Mã OTP hợp lệ"));
    }

    private void handleLogin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        UsersLoginRequest body = ServletUtils.readBody(req, UsersLoginRequest.class);
        AuthTokenResponse tokens = authService.login(body);

        // Vẫn set thêm HttpSession để tương thích ngược với AuthenFilter/AuthorFilter
        // hiện đang check session cho các Servlet khác (Room, User, Voucher...).
        HttpSession session = req.getSession(true);
        session.setAttribute("user", tokens.getUser());
        session.setAttribute("role", tokens.getUser().getRoles());

        ServletUtils.ok(req, resp, tokens);
    }

    private void handleRefreshToken(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        RefreshTokenRequest body = ServletUtils.readBody(req, RefreshTokenRequest.class);
        ValidationUtils.validate(body);
        AuthTokenResponse tokens = authService.refreshToken(body);
        ServletUtils.ok(req, resp, tokens);
    }

    private void handleForgotPassword(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        ForgotPasswordRequest body = ServletUtils.readBody(req, ForgotPasswordRequest.class);
        ValidationUtils.validate(body);
        authService.forgotPassword(body);
        ServletUtils.ok(req, resp, Map.of("message", "Đã gửi mã OTP đặt lại mật khẩu tới email"));
    }

    private void handleResetPassword(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        ResetPasswordRequest body = ServletUtils.readBody(req, ResetPasswordRequest.class);
        ValidationUtils.validate(body);
        authService.resetPassword(body);
        ServletUtils.ok(req, resp, Map.of("message", "Đặt lại mật khẩu thành công, vui lòng đăng nhập lại"));
    }

    private void handleLogout(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        RefreshTokenRequest body = ServletUtils.readBody(req, RefreshTokenRequest.class);
        authService.logout(body);

        HttpSession session = req.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        ServletUtils.ok(req, resp, Map.of("message", "Đăng xuất thành công"));
    }
}
