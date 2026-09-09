package com.vivu.booking.dao;

import com.vivu.booking.entity.LoyaltyRank;
import com.vivu.booking.enums.RankNameType;

import java.util.List;
import java.util.Optional;

public class LoyaltyRankDao extends BaseDao<LoyaltyRank, Long> {

    public LoyaltyRankDao() {
        super(LoyaltyRank.class);
    }

    /** UNIQUE index trên name. */
    public Optional<LoyaltyRank> findByName(RankNameType name) {
        return read(session -> session.createQuery(
                        "SELECT r FROM LoyaltyRank r WHERE r.name = :name", LoyaltyRank.class)
                .setParameter("name", name).uniqueResultOptional());
    }

    /** Toàn bộ hạng theo ngưỡng điểm tăng dần — FE render bảng "5 hạng & quyền lợi". */
    public List<LoyaltyRank> findAllOrdered() {
        return read(s -> s.createQuery(
                "SELECT r FROM LoyaltyRank r ORDER BY r.minPoints ASC", LoyaltyRank.class).getResultList());
    }

    /**
     * Bảng nhỏ (5 hạng) - tìm hạng cao nhất mà user đủ điểm để đạt được,
     * dùng khi tính toán rank sau mỗi lần cộng/trừ điểm.
     */
    public Optional<LoyaltyRank> findHighestRankForPoints(int totalPoints) {
        return read(s -> s.createQuery(
                        "SELECT r FROM LoyaltyRank r WHERE r.minPoints <= :points " +
                                "ORDER BY r.minPoints DESC", LoyaltyRank.class)
                .setParameter("points", totalPoints)
                .setMaxResults(1)
                .getResultList().stream().findFirst());
    }

    /** Hạng kế tiếp chưa đạt (minPoints > totalPoints), null = đang ở hạng cao nhất. */
    public Optional<LoyaltyRank> findNextRank(int totalPoints) {
        return read(s -> s.createQuery(
                        "SELECT r FROM LoyaltyRank r WHERE r.minPoints > :points " +
                                "ORDER BY r.minPoints ASC", LoyaltyRank.class)
                .setParameter("points", totalPoints)
                .setMaxResults(1)
                .getResultList().stream().findFirst());
    }
}
