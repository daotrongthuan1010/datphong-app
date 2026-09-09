package com.vivu.booking.controller;

import com.vivu.booking.dto.request.*;
import com.vivu.booking.dto.response.AuthTokenResponse;
import com.vivu.booking.dto.response.LoginTwoFactorChallengeResponse;
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
 *   POST /api/auth/login             - Public (username+password dung -> tra buoc OTP tiep theo)
 *   POST /api/auth/login/2fa         - Public (loginToken + ma 6 so -> tra token)
 *   POST /api/auth/refresh-token     - Public
 *   POST /api/auth/forgot-password   - Public
 *   POST /api/auth/reset-password    - Public
 *   POST /api/auth/logout            - User (yeu cau da dang nhap)
 *   POST /api/auth/2fa/setup         - User (doi thiet bi: can Bearer/session, sinh secret + QR moi)
 *   POST /api/auth/2fa/confirm       - User (doi thiet bi: xac nhan ma 6 so de bat lai)
 *   POST /api/auth/2fa/disable       - User (can Bearer/session, tat 2FA; lan login sau se quet QR lai)
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
                case "/login/2fa" -> handleLoginTwoFactor(req, resp);
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

    /**
     * Buoc 1 cua dang nhap: kiem tra username + password, LUON tra ve phan thu
     * buoc OTP (ke ca tai khoan chua dang ky app Authenticator: lan dau vua sinh
     * khoa vua tra QR de user quet ngay). Khong cap token o day.
     */
    private void handleLogin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        UsersLoginRequest body = ServletUtils.readBody(req, UsersLoginRequest.class);
        ValidationUtils.validate(body);
        if (body.getUsername() == null || body.getUsername().isBlank()
                || body.getPassword() == null || body.getPassword().isBlank()) {
            throw new BusinessException(400, "Thieu ten dang nhap hoac mat khau");
        }

        LoginTwoFactorChallengeResponse challenge = authService.login(body);
        ServletUtils.ok(req, resp, challenge);
    }

    /**
     * Buoc 2 cua dang nhap: nhap ma 6 so tu Google/Microsoft Authenticator.
     * Lan dau dung ma dung se dong thoi bat 2FA cho tai khoan.
     */
    private void handleLoginTwoFactor(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        LoginTwoFactorRequest body = ServletUtils.readBody(req, LoginTwoFactorRequest.class);
        ValidationUtils.validate(body);

        AuthTokenResponse tokens = authService.completeLogin(body);

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
