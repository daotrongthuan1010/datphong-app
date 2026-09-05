package com.vivu.booking.dao;

import com.vivu.booking.entity.ReviewMedia;

import java.util.List;

public class ReviewMediaDao extends BaseDao<ReviewMedia, Long> {

    public ReviewMediaDao() {
        super(ReviewMedia.class);
    }

    public List<ReviewMedia> findByReviewId(Long reviewId) {
        return read(s -> s.createQuery("from ReviewMedia where review.id = :reviewId order by id", ReviewMedia.class)
                .setParameter("reviewId", reviewId)
                .getResultList());
    }

    /** Gom media của nhiều review trong 1 query (tránh N+1 khi list). */
    public List<ReviewMedia> findByReviewIds(List<Long> reviewIds) {
        if (reviewIds == null || reviewIds.isEmpty()) return List.of();
        return read(s -> s.createQuery("from ReviewMedia where review.id in (:ids) order by id", ReviewMedia.class)
                .setParameterList("ids", reviewIds)
                .getResultList());
    }
}
