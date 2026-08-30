package com.vivu.booking.dao;

import com.vivu.booking.entity.VoucherUsage;
import jakarta.persistence.EntityManager;
import java.util.List;

public class VoucherUsageDao extends BaseDao<VoucherUsage, Long> {

    public VoucherUsageDao() {
        super(VoucherUsage.class);
    }
    public long countByVoucherAndUser(Long voucherId, Long userId, EntityManager em) {
        return em.createQuery(
                        "SELECT COUNT(v) FROM VoucherUsage v WHERE v.voucher.id = :voucherId " +
                                "AND v.user.id = :userId", Long.class)
                .setParameter("voucherId", voucherId)
                .setParameter("userId", userId)
                .getSingleResult();
    }
    public long countByVoucher(Long voucherId, EntityManager em) {
        return em.createQuery(
                        "SELECT COUNT(v) FROM VoucherUsage v WHERE v.voucher.id = :voucherId", Long.class)
                .setParameter("voucherId", voucherId)
                .getSingleResult();
    }

    public List<VoucherUsage> findByBookingId(Long bookingId, EntityManager em) {
        return em.createQuery(
                        "SELECT v FROM VoucherUsage v WHERE v.booking.id = :bookingId", VoucherUsage.class)
                .setParameter("bookingId", bookingId)
                .getResultList();
    }
}
