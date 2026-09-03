package com.vivu.booking.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/** Dùng chung cho POST /api/auth/refresh-token và POST /api/auth/logout. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshTokenRequest {

    @NotBlank(message = "refreshToken không được để trống")
    private String refreshToken;
}
