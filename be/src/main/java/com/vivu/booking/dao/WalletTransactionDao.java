package com.vivu.booking.dao;

import com.vivu.booking.entity.WalletTransaction;
import jakarta.persistence.EntityManager;
import java.util.List;

public class WalletTransactionDao extends BaseDao<WalletTransaction, Long> {

    public WalletTransactionDao() {
        super(WalletTransaction.class);
    }

    /** idx_wallet_transactions_wallet (wallet_id, created_at) - lịch sử giao dịch mới nhất trước */
    public List<WalletTransaction> findByWalletId(Long walletId, int page, int size, EntityManager em) {
        return em.createQuery(
                        "SELECT t FROM WalletTransaction t WHERE t.wallet.id = :walletId " +
                                "ORDER BY t.createdAt DESC", WalletTransaction.class)
                .setParameter("walletId", walletId)
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList();
    }
}
