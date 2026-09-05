package com.vivu.booking.dto.response;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TwoFactorSetupResponse {

    /** Base32 secret - dung cho manual entry neu khong scan QR. */
    private String secret;

    /** otpauth://totp/... - frontend render QRCode tu chuoi nay. */
    private String otpAuthUri;

    /** true neu user da bat 2FA roi. */
    private Boolean enabled;

    private String issuer;
}
