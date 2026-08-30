package com.vivu.booking.dto.response;

import com.vivu.booking.enums.ReviewStatusType;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewResponse {
    private Long id;

    private Long bookingId;
    private String bookingCode;

    private Long userId;
    private String userFullName;

    private Long roomId;
    private String roomName;

    private Short rating;
    private String comment;
    private ReviewStatusType status;

    private LocalDateTime createdAt;
}
