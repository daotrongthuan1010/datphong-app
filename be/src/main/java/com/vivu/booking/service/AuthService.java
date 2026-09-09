package com.vivu.booking.service;

import com.vivu.booking.dto.request.*;
import com.vivu.booking.dto.response.AuthTokenResponse;
import com.vivu.booking.dto.response.LoginTwoFactorChallengeResponse;
import com.vivu.booking.dto.response.TwoFactorSetupResponse;
import com.vivu.booking.dto.response.UsersResponse;

public interface AuthService {

    /** 1. POST /api/auth/register */
    UsersResponse register(RegisterRequest req);

    /** 2. POST /api/auth/otp/send */
    void sendOtp(SendOtpRequest req);

    /** 3. POST /api/auth/otp/verify */
    void verifyOtp(OtpVerifyRequest req);

    /**
     * 4. POST /api/auth/login - KIEM TRA username+password, LUON tra ve buoc OTP
     * tiep theo (khong bao gio cap token o day). Tai khoan chua dang ky app
     * Authenticator se duoc sinh secret + QR de quet ngay lan dau (setupRequired=true).
     */
    LoginTwoFactorChallengeResponse login(UsersLoginRequest req);

    /**
     * 4b. POST /api/auth/login/2fa - nhap ma OTP 6 so de hoan tat dang nhap,
     * tra access/refresh token. Lan xac nhan dau tien cung bat 2FA cho tai khoan.
     */
    AuthTokenResponse completeLogin(LoginTwoFactorRequest req);

    /** 5. POST /api/auth/refresh-token */
    AuthTokenResponse refreshToken(RefreshTokenRequest req);

    /** 6. POST /api/auth/forgot-password */
    void forgotPassword(ForgotPasswordRequest req);

    /** 7. POST /api/auth/reset-password */
    void resetPassword(ResetPasswordRequest req);

    /** 8. POST /api/auth/logout */
    void logout(RefreshTokenRequest req);

    // ---------------------------------------------------------------- 2FA (TOTP - chuan RFC 6238, tuong thich Google + Microsoft Authenticator)

    /** Sinh secret moi cho user (da dang nhap), tra ve otpauth URI de scan QR. */
    TwoFactorSetupResponse setupTwoFactor(Long userId);

    /** Xac nhan ma 6 so lan dau de bat 2FA. */
    void confirmTwoFactor(Long userId, String totpCode);

    /**
     * Tat 2FA, can ma TOTP hien tai de xac thuc. Sau khi tat, lan dang nhap
     * tiep theo se quay lai trang thai choan QR dang ky lai tu dau.
     */
    void disableTwoFactor(Long userId, String totpCode);
}
