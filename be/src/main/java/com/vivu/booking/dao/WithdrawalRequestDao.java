package com.vivu.booking.dao;

import com.vivu.booking.entity.WithdrawalRequest;
import com.vivu.booking.enums.WithdrawalStatusType;
import jakarta.persistence.EntityManager;
import java.util.List;

public class WithdrawalRequestDao extends BaseDao<WithdrawalRequest, Long> {

    public WithdrawalRequestDao() {
        super(WithdrawalRequest.class);
    }

    /** idx_withdrawal_requests_host_id */
    public List<WithdrawalRequest> findByHostId(Long hostId, int page, int size, EntityManager em) {
        return em.createQuery(
                        "SELECT w FROM WithdrawalRequest w WHERE w.host.id = :hostId " +
                                "ORDER BY w.requestedAt DESC", WithdrawalRequest.class)
                .setParameter("hostId", hostId)
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList();
    }

    /** idx_withdrawal_requests_status - Admin duyệt yêu cầu rút tiền đang chờ xử lý */
    public List<WithdrawalRequest> findByStatus(WithdrawalStatusType status, int page, int size,
                                                EntityManager em) {
        return em.createQuery(
                        "SELECT w FROM WithdrawalRequest w WHERE w.status = :status " +
                                "ORDER BY w.requestedAt ASC", WithdrawalRequest.class)
                .setParameter("status", status)
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList();
    }
}
