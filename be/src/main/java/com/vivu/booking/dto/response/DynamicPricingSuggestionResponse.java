package com.vivu.booking.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DynamicPricingSuggestionResponse {
    private Long id;

    private Long roomId;
    private String roomName;

    private LocalDate targetDate;
    private BigDecimal suggestedPrice;
    private String reason;
    private String modelVersion;
    private Boolean applied;

    private LocalDateTime createdAt;
}
