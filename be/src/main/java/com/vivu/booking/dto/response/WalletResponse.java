package com.vivu.booking.dto.response;

import com.vivu.booking.enums.WalletOwnerType;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletResponse {
    private Long id;
    private Long ownerId;
    private WalletOwnerType ownerType;
    private BigDecimal balance;
    private String currency;
}
