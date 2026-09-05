package com.vivu.booking.mapper;

import com.vivu.booking.dto.response.AiRecommendationResponse;
import com.vivu.booking.entity.AI_Recommendations;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AiRecommendationMapper {

    public static AiRecommendationResponse toResponse(AI_Recommendations e) {
        return AiRecommendationResponse.builder()
                .userId(e.getUser() != null ? e.getUser().getId() : null)
                .roomId(e.getRoom() != null ? e.getRoom().getId() : null)
                .roomName(e.getRoom() != null ? e.getRoom().getName() : null)
                .roomCode(e.getRoom() != null ? e.getRoom().getCode() : null)
                .score(e.getScore())
                .modelVersion(e.getModelVersion())
                .generatedAt(e.getGeneratedAt())
                .build();
    }
}
