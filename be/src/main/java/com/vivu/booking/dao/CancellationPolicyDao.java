package com.vivu.booking.dao;

import com.vivu.booking.entity.CancellationPolicy;
import jakarta.persistence.EntityManager;
import java.util.List;

public class CancellationPolicyDao extends BaseDao<CancellationPolicy, Long> {

    public CancellationPolicyDao() {
        super(CancellationPolicy.class);
    }

    /** idx_cancellation_policies_host_id - Host xem danh sách chính sách mình tạo */
    public List<CancellationPolicy> findByHostId(Long hostId, EntityManager em) {
        return em.createQuery(
                        "SELECT c FROM CancellationPolicy c WHERE c.host.id = :hostId", CancellationPolicy.class)
                .setParameter("hostId", hostId)
                .getResultList();
    }

    /** JOIN FETCH các rule đi kèm để tính hoàn tiền trong 1 lần truy vấn */
    public CancellationPolicy findByIdWithRules(Long id, EntityManager em) {
        List<CancellationPolicy> result = em.createQuery(
                        "SELECT DISTINCT c FROM CancellationPolicy c LEFT JOIN FETCH c.rules WHERE c.id = :id",
                        CancellationPolicy.class)
                .setParameter("id", id)
                .getResultList();
        return result.stream().findFirst().orElse(null);
    }
}
