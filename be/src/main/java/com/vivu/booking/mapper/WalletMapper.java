package com.vivu.booking.mapper;

import com.vivu.booking.dto.response.WalletResponse;
import com.vivu.booking.dto.response.WalletTransactionResponse;
import com.vivu.booking.entity.Wallet;
import com.vivu.booking.entity.WalletTransaction;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class WalletMapper {

    public static WalletResponse toResponse(Wallet e) {
        if(e == null) {
            return null;
        }
        return WalletResponse.builder()
                .id(e.getId())
                .userId(e.getUser()!=null?e.getUser().getId():null)
                .username(e.getUser()!=null?e.getUser().getUsername():null)
                .email(e.getUser()!=null?e.getUser().getEmail():null)
                .balance(e.getBalance())
                .currency(e.getCurrency())
                .build();
    }

    public static WalletTransactionResponse toResponse(WalletTransaction e) {
        return WalletTransactionResponse.builder()
                .id(e.getId())
                .walletId(e.getWallet() != null ? e.getWallet().getId() : null)
                .txType(e.getTxType())
                .amount(e.getAmount())
                .referenceType(e.getReferenceType())
                .referenceId(e.getReferenceId())
                .balanceAfter(e.getBalanceAfter())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
