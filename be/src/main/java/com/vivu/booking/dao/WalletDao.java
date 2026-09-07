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

    public Optional<Wallet> findByUserId(Long userId) {
        return read(s -> s.createQuery("""
                select w
                from Wallet w
                join fetch w.user
                where w.user.id = :userId
                """, Wallet.class)
                .setParameter("userId", userId)
                .uniqueResultOptional());
    }
    public boolean existsByUserId(Long userId) {
        Long count = read(s -> s.createQuery("""
                select count(w)
                from Wallet w
                where w.user.id = :userId
                """, Long.class)
                .setParameter("userId", userId)
                .getSingleResult());
        return count != null && count > 0;
    }
    public Optional<Wallet> findByIdWithUser(Long walletId) {
        return read(s -> s.createQuery("""
                select w
                from Wallet w
                join fetch w.user
                where w.id = :walletId
                """, Wallet.class)
                .setParameter("walletId", walletId)
                .uniqueResultOptional());
    }
}
