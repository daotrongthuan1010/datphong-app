package com.vivu.booking.dto.response;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoyaltyProfileResponse {
    /** Tổng điểm tích lũy (SUM point_history.points_change). */
    private long totalPoints;
    /** Hạng hiện tại (null nếu chưa seed ranks). */
    private LoyaltyRankResponse currentRank;
    /** Hạng kế tiếp (null nếu đang ở hạng cao nhất). */
    private LoyaltyRankResponse nextRank;
    /** Điểm còn thiếu để lên hạng kế tiếp (null nếu đã max). */
    private Integer pointsToNextRank;
}
