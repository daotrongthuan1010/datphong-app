package com.vivu.booking.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

/**
 * Tra ve sau khi username+password DUNG nhung van CHUA duoc dang nhap:
 * he thong bat buoc qua buoc OTP (TOTP) ngay tai man login.
 *
 * - setupRequired = true  -> user chua tung dang ky app Authenticator: BE vua sinh
 *   secret moi, tra kem qrCodeDataUri + otpAuthUri + secret de user quet/nhap tay.
 * - setupRequired = false -> user da quet tu truoc: chi can nhap ma 6 so.
 *
 * loginToken la phieu 1 lan (TTL ngan, luu Redis) dung de goi /api/auth/login/2fa,
 * tranh phai gui lai username+password. Access/refresh token CHI duoc cap o buoc do.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginTwoFactorChallengeResponse {

    @Builder.Default
    private Boolean requiresTwoFactor = true;

    /** true = lan dau tien: phai quet QR bang Google/Microsoft Authenticator. */
    private Boolean setupRequired;

    /** Phieu xac nhan OTP - gui kem ma 6 so toi POST /api/auth/login/2fa. */
    private String loginToken;

    /** So giay con lai cua loginToken. */
    private Long expiresIn;

    /** data:image/png;base64,... - chi co khi setupRequired=true. */
    private String qrCodeDataUri;

    /** otpauth://totp/... - chi co khi setupRequired=true (dung cho "nhap ma thu cong"). */
    private String otpAuthUri;

    /** Khoa bi mat Base32 - chi co khi setupRequired=true, hien cho user nhap tay neu khong quet duoc QR. */
    private String secret;

    private String issuer;

    private String username;
}
