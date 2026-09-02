package com.vivu.booking.mapper;

import com.vivu.booking.dto.response.AiRecommendationResponse;
import com.vivu.booking.entity.AI_Recommendations;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AiRecommendationMapper {

    public static AiRecommendationResponse toResponse(AI_Recommendations e) {
        return AiRecommendationResponse.builder()
                .user(e.getUser())
                //.room(e.getRoom())
                .score(e.getScore())
                .modelVersion(e.getModelVersion())
                .generatedAt(e.getGeneratedAt())
                .build();
    }
}
