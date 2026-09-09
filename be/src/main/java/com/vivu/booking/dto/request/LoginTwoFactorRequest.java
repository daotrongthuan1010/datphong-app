package com.vivu.booking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

/**
 * Buoc 2 cua dang nhap: POST /api/auth/login/2fa
 * loginToken do POST /api/auth/login tra ve sau khi username+password dung.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginTwoFactorRequest {

    @NotBlank(message = "Thieu loginToken - vui long dang nhap lai")
    private String loginToken;

    @NotBlank(message = "Ma OTP khong duoc de trong")
    @Pattern(regexp = "^[0-9]{6}$", message = "Ma OTP phai la 6 chu so")
    private String code;
}
