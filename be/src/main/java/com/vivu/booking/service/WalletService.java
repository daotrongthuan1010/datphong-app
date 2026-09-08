package com.vivu.booking.service;

import com.vivu.booking.dto.request.WalletRequest;
import com.vivu.booking.dto.request.WalletTransactionRequest;
import com.vivu.booking.dto.response.WalletResponse;
import com.vivu.booking.dto.response.WalletTransactionResponse;

import java.math.BigDecimal;
import java.util.List;

public interface WalletService {
    WalletResponse create(Long userId, WalletRequest request);
    WalletResponse getMyWallet(Long userId);
    List<WalletTransactionResponse> getMyTransactions(Long userId);
    WalletTransactionResponse update(Long userId, WalletTransactionRequest request);
    WalletTransactionResponse delete(Long userId, WalletTransactionRequest request);
}
