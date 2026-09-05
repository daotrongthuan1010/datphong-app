package com.vivu.booking.mapper;

import com.vivu.booking.dto.response.BookingResponse;
import com.vivu.booking.entity.Booking;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BookingMapper {
    public static BookingResponse toResponse(Booking b) {
        return BookingResponse.builder()
                .id(b.getId())
                .bookingCode(b.getBookingCode())
                .userId(b.getUser() != null ? b.getUser().getId() : null)
                .userFullName(b.getUser() != null ? b.getUser().getFullName() : null)
                .roomId(b.getRoom() != null ? b.getRoom().getId() : null)
                .roomName(b.getRoom() != null ? b.getRoom().getName() : null)
                .roomCode(b.getRoom() != null ? b.getRoom().getCode() : null)
                .checkinDate(b.getCheckinDate())
                .checkoutDate(b.getCheckoutDate())
                .guestsCount(b.getGuestsCount())
                .status(b.getStatus())
                .holdExpiresAt(b.getHoldExpiresAt())
                .totalPrice(b.getTotalPrice())
                .currency(b.getCurrency())
                .voucherCode(b.getVoucher() != null ? b.getVoucher().getCode() : null)
                .loyaltyDiscountPercent(b.getLoyaltyDiscountPercent())
                .createdAt(b.getCreatedAt())
                .updatedAt(b.getUpdatedAt())
                .build();
    }
}
