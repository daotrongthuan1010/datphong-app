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
                .booking(e.getBooking())
                .user(e.getUser())
                .room(e.getRoom())
                .rating(e.getRating())
                .comment(e.getComment())
                .status(e.getStatus())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
