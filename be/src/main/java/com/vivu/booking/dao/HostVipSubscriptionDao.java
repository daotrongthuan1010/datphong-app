package com.vivu.booking.dao;

import com.vivu.booking.entity.HostVipSubscription;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;

public class HostVipSubscriptionDao extends BaseDao<HostVipSubscription, Long> {

    public HostVipSubscriptionDao() {
        super(HostVipSubscription.class);
    }

    /** idx_host_vip_subscriptions_host */
    public List<HostVipSubscription> findByHostId(Long hostId, EntityManager em) {
        return em.createQuery(
                        "SELECT s FROM HostVipSubscription s WHERE s.host.id = :hostId " +
                                "ORDER BY s.endDate DESC", HostVipSubscription.class)
                .setParameter("hostId", hostId)
                .getResultList();
    }

    /**
     * idx_host_vip_subscriptions_exp (status, end_date) WHERE status='active' - job nền
     * quét các gói VIP sắp hết hạn để gửi thông báo gia hạn.
     */
    public List<HostVipSubscription> findExpiringBefore(LocalDate date, EntityManager em) {
        return em.createQuery(
                        "SELECT s FROM HostVipSubscription s WHERE " +
                                "s.status = com.vivu.booking.enums.VipSubStatusType.ACTIVE " +
                                "AND s.endDate <= :date", HostVipSubscription.class)
                .setParameter("date", date)
                .getResultList();
    }
}
