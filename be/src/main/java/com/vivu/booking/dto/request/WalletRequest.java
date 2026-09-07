package com.vivu.booking.dto.request;

import jakarta.validation.constraints.Pattern;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletRequest {
    @Pattern(regexp = "VND|USD", message = "Tiền tệ chỉ hỗ trợ VND hoặc USD")
    private String currency = "VND";
}
