package com.vivu.booking.dao;

import com.vivu.booking.entity.Wallet;
import com.vivu.booking.enums.WalletOwnerType;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;

public class WalletDao extends BaseDao<Wallet, Long> {

    public WalletDao() {
        super(Wallet.class);
    }

    /** UNIQUE(owner_id, owner_type) - lấy đúng 1 ví của user/host */
    public Optional<Wallet> findByOwner(Long ownerId, WalletOwnerType ownerType, EntityManager em) {
        List<Wallet> result = em.createQuery(
                        "SELECT w FROM Wallet w WHERE w.ownerId = :ownerId AND w.ownerType = :ownerType",
                        Wallet.class)
                .setParameter("ownerId", ownerId)
                .setParameter("ownerType", ownerType)
                .getResultList();
        return result.stream().findFirst();
    }

    /**
     * Cộng/trừ số dư nguyên tử bằng bulk update - tránh lost-update khi 2 giao dịch
     * cùng ghi vào 1 ví song song (an toàn hơn nhiều so với load - cộng tay - save).
     */
    public int adjustBalance(Long walletId, java.math.BigDecimal delta, EntityManager em) {
        return em.createQuery("UPDATE Wallet w SET w.balance = w.balance + :delta WHERE w.id = :id")
                .setParameter("delta", delta)
                .setParameter("id", walletId)
                .executeUpdate();
    }
}
