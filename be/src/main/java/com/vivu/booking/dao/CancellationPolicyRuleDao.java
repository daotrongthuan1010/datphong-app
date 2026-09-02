package com.vivu.booking.dao;

import com.vivu.booking.entity.CancellationPolicyRule;
import jakarta.persistence.EntityManager;
import java.util.List;

public class CancellationPolicyRuleDao extends BaseDao<CancellationPolicyRule, Long> {

    public CancellationPolicyRuleDao() {
        super(CancellationPolicyRule.class);
    }

    /**
     * idx_cancellation_policy_rules_policy - sắp xếp giảm dần theo số ngày trước check-in
     * để tìm rule đầu tiên khớp với số ngày còn lại khi khách huỷ phòng.
     */
    public List<CancellationPolicyRule> findByPolicyIdOrdered(Long policyId, EntityManager em) {
        return em.createQuery(
                        "SELECT r FROM CancellationPolicyRule r WHERE r.policy.id = :policyId " +
                                "ORDER BY r.daysBeforeCheckin DESC", CancellationPolicyRule.class)
                .setParameter("policyId", policyId)
                .getResultList();
    }
}
