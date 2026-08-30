package com.vivu.booking.dao;

import com.vivu.booking.entity.DisputeEvidence;
import jakarta.persistence.EntityManager;
import java.util.List;

public class DisputeEvidenceDao extends BaseDao<DisputeEvidence, Long> {

    public DisputeEvidenceDao() {
        super(DisputeEvidence.class);
    }

    /** idx_dispute_evidence_ticket_id */
    public List<DisputeEvidence> findByTicketId(Long ticketId, EntityManager em) {
        return em.createQuery(
                        "SELECT e FROM DisputeEvidence e WHERE e.ticket.id = :ticketId " +
                                "ORDER BY e.createdAt ASC", DisputeEvidence.class)
                .setParameter("ticketId", ticketId)
                .getResultList();
    }

    /** idx_dispute_evidence_uploaded_by */
    public List<DisputeEvidence> findByUploadedBy(Long userId, EntityManager em) {
        return em.createQuery(
                        "SELECT e FROM DisputeEvidence e WHERE e.uploadedBy.id = :userId", DisputeEvidence.class)
                .setParameter("userId", userId)
                .getResultList();
    }
}
