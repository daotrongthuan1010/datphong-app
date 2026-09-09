package com.vivu.booking.mapper;

import com.vivu.booking.dto.response.PointHistoryResponse;
import com.vivu.booking.entity.PointHistory;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PointHistoryMapper {

    public static PointHistoryResponse toResponse(PointHistory e) {
        return PointHistoryResponse.builder()
                .id(e.getId())
                .userId(e.getUser() != null ? e.getUser().getId() : null)
                .bookingId(e.getBooking() != null ? e.getBooking().getId() : null)
                .bookingCode(e.getBooking() != null ? e.getBooking().getBookingCode() : null)
                .pointsChange(e.getPointsChange())
                .reason(e.getReason())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
