package com.vivu.booking.dao;

import com.vivu.booking.entity.WalletTransaction;
import com.vivu.booking.enums.WalletTxType;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;

public class WalletTransactionDao extends BaseDao<WalletTransaction, Long> {

    public WalletTransactionDao() {
        super(WalletTransaction.class);
    }

    /** idx_wallet_transactions_wallet (wallet_id, created_at) - lịch sử giao dịch mới nhất trước */
    public List<WalletTransaction> findByWalletId(Long walletId) {
        return read(s -> s.createQuery("""
                select wt
                from WalletTransaction wt
                join fetch wt.wallet w
                where w.id = :walletId
                order by wt.createdAt desc
                """, WalletTransaction.class)
                .setParameter("walletId", walletId)
                .getResultList());
    }
    public boolean existsByReference(Long walletId, WalletTxType txType, String referenceType, Long referenceId) {
        Long count = read(session -> session.createQuery("""
                select count(wt)
                from WalletTransaction wt
                where wt.wallet.id = :walletId
                  and wt.txType = :txType
                  and wt.referenceType = :referenceType
                  and wt.referenceId = :referenceId
                """, Long.class)

                .setParameter("walletId", walletId)
                .setParameter("txType", txType)
                .setParameter("referenceType", referenceType)
                .setParameter("referenceId", referenceId)
                .getSingleResult()
        );

        return count != null && count > 0;
    }
}
