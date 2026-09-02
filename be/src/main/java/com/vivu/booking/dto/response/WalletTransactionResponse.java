package com.vivu.booking.dto.response;

import com.vivu.booking.enums.WalletTxType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletTransactionResponse {
    private Long id;
    private Long walletId;
    private WalletTxType txType;
    private BigDecimal amount;
    private String referenceType;
    private Long referenceId;
    private BigDecimal balanceAfter;
    private LocalDateTime createdAt;
}
