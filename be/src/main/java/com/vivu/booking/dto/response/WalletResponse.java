package com.vivu.booking.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletResponse {
    private Long id;
    private Long userId;
    private String username;
    private String email;
    private BigDecimal balance;
    private String currency;
}
