package com.vivu.booking.dao;

import com.vivu.booking.entity.Review;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;

public class ReviewDao extends BaseDao<Review, Long> {

    public ReviewDao() {
        super(Review.class);
    }

    /**
     * idx_reviews_room_visible (partial index WHERE status='visible') - chỉ quét đúng
     * tập review đang hiển thị công khai, không lãng phí index cho review đã bị ẩn.
     */
    public List<Review> findVisibleByRoomId(Long roomId, int page, int size, EntityManager em) {
        return em.createQuery(
                        "SELECT r FROM Review r WHERE r.room.id = :roomId " +
                                "AND r.status = com.vivu.booking.enums.ReviewStatusType.VISIBLE " +
                                "ORDER BY r.createdAt DESC", Review.class)
                .setParameter("roomId", roomId)
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList();
    }

    /** idx_reviews_user_id */
    public List<Review> findByUserId(Long userId, int page, int size, EntityManager em) {
        return em.createQuery(
                        "SELECT r FROM Review r WHERE r.user.id = :userId ORDER BY r.createdAt DESC",
                        Review.class)
                .setParameter("userId", userId)
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList();
    }

    /** UNIQUE(booking_id) - mỗi booking chỉ được review 1 lần */
    public Optional<Review> findByBookingId(Long bookingId, EntityManager em) {
        List<Review> result = em.createQuery(
                        "SELECT r FROM Review r WHERE r.booking.id = :bookingId", Review.class)
                .setParameter("bookingId", bookingId)
                .getResultList();
        return result.stream().findFirst();
    }

    /** Tính rating trung bình ngay tại DB thay vì kéo hết bản ghi về tính ở tầng Java. */
    public Double avgRatingByRoom(Long roomId, EntityManager em) {
        return em.createQuery(
                        "SELECT AVG(r.rating) FROM Review r WHERE r.room.id = :roomId " +
                                "AND r.status = com.vivu.booking.enums.ReviewStatusType.VISIBLE", Double.class)
                .setParameter("roomId", roomId)
                .getSingleResult();
    }
}
