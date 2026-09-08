package com.vivu.booking.mapper;
import com.vivu.booking.dto.response.WalletTransactionResponse;
import com.vivu.booking.entity.WalletTransaction;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class WalletTransactionMapper {
    public static WalletTransactionResponse toResponse(
            WalletTransaction transaction
    ) {
        if (transaction == null) {
            return null;
        }

        return WalletTransactionResponse.builder()
                .id(transaction.getId())
                .walletId(transaction.getWallet() != null ? transaction.getWallet().getId() : null)
                .txType(transaction.getTxType())
                .amount(transaction.getAmount())
                .referenceType(transaction.getReferenceType())
                .referenceId(transaction.getReferenceId())
                .balanceAfter(transaction.getBalanceAfter())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}
