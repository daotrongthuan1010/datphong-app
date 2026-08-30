package com.vivu.booking.dao;

import com.vivu.booking.entity.KycDocument;
import com.vivu.booking.enums.DocReviewStatus;
import jakarta.persistence.EntityManager;
import java.util.List;

public class KycDocumentDao extends BaseDao<KycDocument, Long> {

    public KycDocumentDao() {
        super(KycDocument.class);
    }
    public List<KycDocument> findByUserId(Long userId, EntityManager em) {
        return em.createQuery(
                        "SELECT k FROM KycDocument k WHERE k.user.id = :userId ORDER BY k.createdAt DESC",
                        KycDocument.class)
                .setParameter("userId", userId)
                .getResultList();
    }

    public List<KycDocument> findByStatus(DocReviewStatus status, int page, int size, EntityManager em) {
        return em.createQuery(
                        "SELECT k FROM KycDocument k WHERE k.status = :status ORDER BY k.createdAt ASC",
                        KycDocument.class)
                .setParameter("status", status)
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList();
    }
}