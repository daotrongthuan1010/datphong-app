package com.vivu.booking.dao;

import com.vivu.booking.entity.ReviewMedia;
import jakarta.persistence.EntityManager;
import java.util.List;

public class ReviewMediaDao extends BaseDao<ReviewMedia, Long> {

    public ReviewMediaDao() {
        super(ReviewMedia.class);
    }

    /** idx_review_media_review_id */
    public List<ReviewMedia> findByReviewId(Long reviewId, EntityManager em) {
        return em.createQuery(
                        "SELECT m FROM ReviewMedia m WHERE m.review.id = :reviewId", ReviewMedia.class)
                .setParameter("reviewId", reviewId)
                .getResultList();
    }
}
