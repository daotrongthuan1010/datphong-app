package com.vivu.booking.mapper;

import com.vivu.booking.dto.response.LoyaltyRankResponse;
import com.vivu.booking.entity.LoyaltyRank;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class LoyaltyMapper {

    public static LoyaltyRankResponse toResponse(LoyaltyRank e) {
        return LoyaltyRankResponse.builder()
                .id(e.getId())
                .name(e.getName())
                .minPoints(e.getMinPoints())
                .discountPercent(e.getDiscountPercent())
                .benefits(e.getBenefits())
                .build();
    }
}
