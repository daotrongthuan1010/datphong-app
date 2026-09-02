package com.vivu.booking.dto.response;

import com.vivu.booking.entity.Role;
import com.vivu.booking.entity.User;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiRecommendationResponse {
    private User user;
    private Role role; // xem cảnh báo (2) ở trên - lẽ ra phải là Room
    private Double score;
    private String modelVersion;
    private LocalDateTime generatedAt;
}
