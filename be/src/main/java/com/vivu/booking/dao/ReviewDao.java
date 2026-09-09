package com.vivu.booking.dao;

import com.vivu.booking.entity.Review;
import com.vivu.booking.enums.ReviewStatusType;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ReviewDao extends BaseDao<Review, Long> {

    public ReviewDao() {
        super(Review.class);
    }

    /** Lấy kèm user/booking/room để map response an toàn (tránh LazyInitializationException sau khi Session đóng). */
    public Optional<Review> findByIdWithDetails(Long id) {
        return read(s -> s.createQuery("""
                select distinct r from Review r
                left join fetch r.user
                left join fetch r.booking
                left join fetch r.room
                where r.id = :id
                """, Review.class)
                .setParameter("id", id)
                .uniqueResultOptional());
    }

    /** Review đang hiển thị của 1 phòng, mới nhất trước (fetch user + booking + room để map sau khi session đóng). */
    public List<Review> findVisibleByRoomId(Long roomId, int page, int size) {
        return read(s -> s.createQuery("""
                select distinct r from Review r
                left join fetch r.user
                left join fetch r.booking
                left join fetch r.room
                where r.room.id = :roomId and r.status = :visible
                order by r.id desc
                """, Review.class)
                .setParameter("roomId", roomId)
                .setParameter("visible", ReviewStatusType.VISIBLE)
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList());
    }

    public long countVisibleByRoomId(Long roomId) {
        return read(s -> s.createQuery(
                        "select count(r) from Review r where r.room.id = :roomId and r.status = :visible", Long.class)
                .setParameter("roomId", roomId)
                .setParameter("visible", ReviewStatusType.VISIBLE)
                .getSingleResult());
    }

    /** Điểm trung bình ngay tại DB, chỉ tính review đang hiển thị. */
    public Double avgRatingByRoom(Long roomId) {
        return read(s -> s.createQuery("""
                select avg(r.rating) from Review r
                where r.room.id = :roomId and r.status = :visible
                """, Double.class)
                .setParameter("roomId", roomId)
                .setParameter("visible", ReviewStatusType.VISIBLE)
                .getSingleResult());
    }

    /** UNIQUE(booking_id) — mỗi booking chỉ được review 1 lần. */
    public Optional<Review> findByBookingId(Long bookingId) {
        return read(s -> s.createQuery("from Review where booking.id = :bookingId", Review.class)
                .setParameter("bookingId", bookingId)
                .uniqueResultOptional());
    }

    /**
     * Gộp điểm + số review VISIBLE của nhiều phòng trong 1 query (tránh N+1 khi render list Home).
     *
     * @return map roomId -> [avgRating, count]; phòng chưa có review thì không có trong map.
     */
    public Map<Long, double[]> ratingStatsByRoomIds(Collection<Long> roomIds) {
        if (roomIds == null || roomIds.isEmpty()) return Map.of();
        List<Object[]> rows = read(s -> s.createQuery("""
                select r.room.id, avg(r.rating), count(r.id) from Review r
                where r.room.id in :ids and r.status = :visible
                group by r.room.id
                """, Object[].class)
                .setParameter("ids", roomIds)
                .setParameter("visible", ReviewStatusType.VISIBLE)
                .getResultList());
        Map<Long, double[]> out = new HashMap<>();
        for (Object[] row : rows) {
            out.put(((Number) row[0]).longValue(),
                    new double[]{((Number) row[1]).doubleValue(), ((Number) row[2]).doubleValue()});
        }
        return out;
    }
}
