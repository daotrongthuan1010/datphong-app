package com.vivu.booking.dto.request;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TwoFactorVerifyRequest {

    @jakarta.validation.constraints.NotBlank(message = "Ma 2FA khong duoc de trong")
    @jakarta.validation.constraints.Pattern(regexp = "^[0-9]{6}$", message = "Ma 2FA phai la 6 chu so")
    private String code;
}
