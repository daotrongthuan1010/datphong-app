package com.vivu.booking.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PointHistoryResponse {
    private Long id;
    private Long userId;
    private Long bookingId;
    private String bookingCode;
    private Integer pointsChange;
    private String reason;
    private LocalDateTime createdAt;
}
