package com.vivu.booking.service.impl;

import com.vivu.booking.dao.UsersDao;
import com.vivu.booking.dao.WalletDao;
import com.vivu.booking.dao.WalletTransactionDao;
import com.vivu.booking.dto.request.WalletRequest;
import com.vivu.booking.dto.request.WalletTransactionRequest;
import com.vivu.booking.dto.response.WalletResponse;
import com.vivu.booking.dto.response.WalletTransactionResponse;
import com.vivu.booking.entity.User;
import com.vivu.booking.entity.Wallet;
import com.vivu.booking.entity.WalletTransaction;
import com.vivu.booking.enums.WalletTxType;
import com.vivu.booking.exception.BusinessException;
import com.vivu.booking.exception.ResourceNotFoundException;
import com.vivu.booking.mapper.WalletMapper;
import com.vivu.booking.mapper.WalletTransactionMapper;
import com.vivu.booking.service.WalletService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public class WalletServiceImpl implements WalletService {
    private final WalletDao walletDao;
    private final WalletTransactionDao walletTransactionDao;
    private final UsersDao usersDao;

    public WalletServiceImpl(WalletDao walletDao, WalletTransactionDao walletTransactionDao, UsersDao usersDao) {
        this.walletDao = walletDao;
        this.walletTransactionDao = walletTransactionDao;
        this.usersDao = usersDao;
    }
    public WalletServiceImpl() {this(new WalletDao(), new WalletTransactionDao(), new UsersDao());
    }
//tạo ví
    @Override
    public WalletResponse create(Long userId, WalletRequest request) {
        if(userId == null) {
            throw new BusinessException(401,"Không xác định người dùng");
        }
        User user = usersDao.findById(userId)
                .orElseThrow(()->new ResourceNotFoundException("User not found"+userId));
        if(walletDao.existsByUserId(userId)) {
            throw new BusinessException(409,"User đã có ví");
        }
        String currency="VND";
        if(request!=null && request.getCurrency()!=null && !request.getCurrency().isBlank()) {
            currency=request.getCurrency().toUpperCase();
        }
        Wallet wallet = Wallet.builder()
                .user(user)
                .balance(BigDecimal.ZERO)
                .currency(currency)
                .build();
        walletDao.save(wallet);
        return WalletMapper.toResponse(wallet);
    }
//xem ví
    @Override
    public WalletResponse getMyWallet(Long userId) {
        Wallet wallet=getWallet(userId);
        return WalletMapper.toResponse(wallet);
    }

    @Override
    public List<WalletTransactionResponse> getMyTransactions(Long userId) {
        Wallet wallet=getWallet(userId);
        return walletTransactionDao
                .findByWalletId(wallet.getId())
                .stream()
                .map(WalletTransactionMapper::toResponse)
                .toList();
    }
//côn tiền
    @Override
    public WalletTransactionResponse update(Long userId, WalletTransactionRequest request) {
        validateTransactionRequest(request);
        Wallet wallet=getWallet(userId);
        checkDuplicate(wallet.getId(),WalletTxType.CREDIT,request);
        BigDecimal newBalance=wallet.getBalance().add(request.getAmount());
        wallet.setBalance(newBalance);
        walletDao.update(wallet);
        WalletTransaction transaction=WalletTransaction.builder()
                .wallet(wallet)
                .txType(WalletTxType.CREDIT)
                .amount(request.getAmount())
                .referenceType(request.getReferenceType())
                .referenceId(request.getReferenceId())
                .balanceAfter(newBalance)
                .build();
        walletTransactionDao.save(transaction);
        return WalletTransactionMapper.toResponse(transaction);
    }
// trừ tiền
    @Override
    public WalletTransactionResponse delete(Long userId, WalletTransactionRequest request) {
        validateTransactionRequest(request);
        Wallet wallet=getWallet(userId);
        if(wallet.getBalance().compareTo(request.getAmount())<0) {
            throw new BusinessException(400,"Số dư không đủ");
        }
        checkDuplicate(wallet.getId(),WalletTxType.DEBIT,request);
        BigDecimal newBalance=wallet.getBalance().subtract(request.getAmount());
        wallet.setBalance(newBalance);
        walletDao.update(wallet);
        WalletTransaction transaction=WalletTransaction.builder()
                .wallet(wallet)
                .txType(WalletTxType.DEBIT)
                .amount(request.getAmount())
                .referenceType(request.getReferenceType())
                .referenceId(request.getReferenceId())
                .balanceAfter(newBalance)
                .build();
        walletTransactionDao.save(transaction);
        return WalletTransactionMapper.toResponse(transaction);
    }
    private Wallet getWallet(Long userId) {
        if (userId == null) {throw new BusinessException(401, "Không xác định được người dùng");}
        return walletDao.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for user: " + userId));
    }
    private void validateTransactionRequest(WalletTransactionRequest request) {
        if (request == null) {throw new BusinessException(400, "Thông tin giao dịch không được để trống");}
        if (request.getAmount() == null) {throw new BusinessException(400, "Số tiền không được để trống");}
        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(400, "Số tiền phải lớn hơn 0");
        }
    }
    private void checkDuplicate(Long walletId, WalletTxType txType, WalletTransactionRequest request) {
        if (request.getReferenceType() == null || request.getReferenceId() == null) {return;}
        boolean exists = walletTransactionDao.existsByReference(
                        walletId,
                        txType,
                        request.getReferenceType(),
                        request.getReferenceId()
                );
        if (exists) {throw new BusinessException(409, "Giao dịch này đã được xử lý");}
    }
}
