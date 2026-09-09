package com.vivu.booking.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TwoFactorSetupResponse {

    /** Base32 secret - dung cho manual entry neu khong scan QR. */
    private String secret;

    /** otpauth://totp/... - frontend render QRCode tu chuoi nay. */
    private String otpAuthUri;

    /**
     * data:image/png;base64,... - QR da sinh san tu BE (zxing),
     * FE chi can <img src={giaTriNay}/> la user quet duoc ngay.
     */
    private String qrCodeDataUri;

    /** true neu user da bat 2FA roi. */
    private Boolean enabled;

    private String issuer;
}
