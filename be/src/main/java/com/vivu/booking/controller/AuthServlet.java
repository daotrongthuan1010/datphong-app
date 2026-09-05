package com.vivu.booking.controller;

import com.vivu.booking.dto.request.*;
import com.vivu.booking.dto.response.AuthTokenResponse;
import com.vivu.booking.dto.response.TwoFactorSetupResponse;
import com.vivu.booking.dto.response.UsersResponse;
import com.vivu.booking.exception.BusinessException;
import com.vivu.booking.service.AuthService;
import com.vivu.booking.service.impl.AuthServiceImpl;
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
 * Gom cac API xac thuc vao 1 Servlet, phan biet qua pathInfo:
 *
 *   POST /api/auth/register          - Public
 *   POST /api/auth/otp/send          - Public
 *   POST /api/auth/otp/verify        - Public
 *   POST /api/auth/login             - Public (neu da bat 2FA can them totpCode)
 *   POST /api/auth/refresh-token     - Public
 *   POST /api/auth/forgot-password   - Public
 *   POST /api/auth/reset-password    - Public
 *   POST /api/auth/logout            - User (yeu cau da dang nhap)
 *   POST /api/auth/2fa/setup         - User (can Bearer token / session, sinh secret + QR)
 *   POST /api/auth/2fa/confirm       - User (can Bearer token / session, xac nhan ma 6 so de bat)
 *   POST /api/auth/2fa/disable       - User (can Bearer token / session, tat 2FA)
 *
 * 2FA (TOTP) tuong thich Google Authenticator + Microsoft Authenticator (RFC 6238).
 * QUAN TRONG: login co field tuy chon totpCode - bat buoc khi tai khoan da bat 2FA.
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
                ServletUtils.writeJson(resp, 404, Map.of("success", false, "message", "Thieu path con cua /api/auth"));
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
                case "/2fa/setup" -> handle2faSetup(req, resp);
                case "/2fa/confirm" -> handle2faConfirm(req, resp);
                case "/2fa/disable" -> handle2faDisable(req, resp);
                default -> ServletUtils.writeJson(resp, 404,
                        Map.of("success", false, "message", "Khong tim thay endpoint: /api/auth" + path));
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
        ServletUtils.ok(req, resp, Map.of("message", "Da gui ma OTP, vui long kiem tra email"));
    }

    private void handleVerifyOtp(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        OtpVerifyRequest body = ServletUtils.readBody(req, OtpVerifyRequest.class);
        ValidationUtils.validate(body);
        authService.verifyOtp(body);
        ServletUtils.ok(req, resp, Map.of("valid", true, "message", "Ma OTP hop le"));
    }

    private void handleLogin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        UsersLoginRequest body = ServletUtils.readBody(req, UsersLoginRequest.class);
        AuthTokenResponse tokens = authService.login(body);

        // Van set them HttpSession de tuong thich nguoc voi AuthenFilter/AuthorFilter
        // hien dang check session cho cac Servlet khac (Room, User, Voucher...).
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
        ServletUtils.ok(req, resp, Map.of("message", "Da gui ma OTP dat lai mat khau toi email"));
    }

    private void handleResetPassword(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        ResetPasswordRequest body = ServletUtils.readBody(req, ResetPasswordRequest.class);
        ValidationUtils.validate(body);
        authService.resetPassword(body);
        ServletUtils.ok(req, resp, Map.of("message", "Dat lai mat khau thanh cong, vui long dang nhap lai"));
    }

    private void handleLogout(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        RefreshTokenRequest body = ServletUtils.readBody(req, RefreshTokenRequest.class);
        authService.logout(body);

        HttpSession session = req.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        ServletUtils.ok(req, resp, Map.of("message", "Dang xuat thanh cong"));
    }

    // ---------------------------------------------------------------- 2FA handlers

    private void handle2faSetup(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long userId = requireAuth(req);
        TwoFactorSetupResponse result = authService.setupTwoFactor(userId);
        ServletUtils.ok(req, resp, result);
    }

    private void handle2faConfirm(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long userId = requireAuth(req);
        TwoFactorVerifyRequest body = ServletUtils.readBody(req, TwoFactorVerifyRequest.class);
        ValidationUtils.validate(body);
        authService.confirmTwoFactor(userId, body.getCode());
        ServletUtils.ok(req, resp, Map.of("success", true, "message", "Bat xac thuc 2 lop thanh cong"));
    }

    private void handle2faDisable(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long userId = requireAuth(req);
        TwoFactorVerifyRequest body = ServletUtils.readBody(req, TwoFactorVerifyRequest.class);
        ValidationUtils.validate(body);
        authService.disableTwoFactor(userId, body.getCode());
        ServletUtils.ok(req, resp, Map.of("success", true, "message", "Da tat xac thuc 2 lop"));
    }

    /**
     * Lay userId tu Bearer access token (header Authorization) hoac fallback sang HttpSession.
     * Nhem cac endpoint 2FA nam duoi /api/auth/* da duoc whitelist khoi AuthenFilter
     * nen khong the dua vao filter de xac thuc.
     */
    private Long requireAuth(HttpServletRequest req) {
        // 1) Authorization: Bearer <accessToken>
        String header = req.getHeader("Authorization");
        if (header != null && header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            String token = header.substring(7).trim();
            JwtUtil.Claims claims = JwtUtil.parse(token);
            if (!"access".equals(claims.getType())) {
                throw new BusinessException(401, "Token khong phai access token");
            }
            return claims.getUserId();
        }
        // 2) Fallback: HttpSession (tuong thich nguoc voi cac san pham dang dung session)
        HttpSession sess = req.getSession(false);
        if (sess != null) {
            Object userAttr = sess.getAttribute("user");
            if (userAttr instanceof com.vivu.booking.dto.response.AuthTokenResponse.UserSummary us && us.getId() != null) {
                return us.getId();
            }
            if (userAttr instanceof com.vivu.booking.dto.response.UsersLoginResponse ul && ul.getId() != null) {
                return ul.getId();
            }
        }
        throw new BusinessException(401, "Chua dang nhap - gui Authorization: Bearer <accessToken>");
    }
}
