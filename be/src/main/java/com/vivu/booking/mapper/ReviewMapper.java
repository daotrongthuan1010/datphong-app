package com.vivu.booking.mapper;

import com.vivu.booking.dto.request.ReviewCreateRequest;
import com.vivu.booking.dto.response.ReviewResponse;
import com.vivu.booking.entity.Review;
import com.vivu.booking.enums.ReviewStatusType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ReviewMapper {

    /** booking/user/room do Service gắn (lấy từ booking đã COMPLETED, không tin client gửi thẳng roomId/userId). */
    public static Review toEntity(ReviewCreateRequest req) {
        return Review.builder()
                .rating(req.getRating())
                .comment(req.getComment())
                .status(ReviewStatusType.VISIBLE)
                .build();
    }

    public static ReviewResponse toResponse(Review e) {
        return ReviewResponse.builder()
                .id(e.getId())
                .bookingId(e.getBooking() != null ? e.getBooking().getId() : null)
                .bookingCode(e.getBooking() != null ? e.getBooking().getBookingCode() : null)
                .userId(e.getUser() != null ? e.getUser().getId() : null)
                .userFullName(e.getUser() != null ? e.getUser().getFullName() : null)
                .roomId(e.getRoom() != null ? e.getRoom().getId() : null)
                .roomName(e.getRoom() != null ? e.getRoom().getName() : null)
                .rating(e.getRating())
                .comment(e.getComment())
                .status(e.getStatus())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
