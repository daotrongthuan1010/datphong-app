package com.vivu.booking.service;

import com.vivu.booking.dto.request.WalletRequest;
import com.vivu.booking.dto.response.WalletResponse;
import com.vivu.booking.dto.response.WalletTransactionResponse;

import java.math.BigDecimal;
import java.util.List;

public interface WalletService {
    WalletResponse create(Long userId, WalletRequest request);
    List<WalletTransactionResponse> getMyTransactions(Long userId);
    WalletTransactionResponse update(Long userId, BigDecimal amount, String referenceType,Long referenceId,String description);
    WalletTransactionResponse delete(Long userId, BigDecimal amount,String referenceType,Long referenceId,String description);
}
