package com.vivu.booking.mapper;

import com.vivu.booking.dto.request.BookingCreateRequest;
import com.vivu.booking.dto.response.BookingResponse;
import com.vivu.booking.entity.Booking;
import com.vivu.booking.enums.BookingStatusType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BookingMapper {

    /**
     * Chỉ set các field scalar có sẵn trong request. user/room/voucher/totalPrice/
     * bookingCode PHẢI được Service gắn tiếp sau khi lookup DB & tính giá -
     * Mapper không tự query DB.
     */
    public static Booking toEntity(BookingCreateRequest req) {
        return Booking.builder()
                .checkinDate(req.getCheckinDate())
                .checkoutDate(req.getCheckoutDate())
                .guestsCount(req.getGuestsCount())
                .status(BookingStatusType.HOLD)
                .totalPrice(BigDecimal.ZERO) // Service sẽ set lại giá trị thật
                .build();
    }

    public static BookingResponse toResponse(Booking e) {
        return BookingResponse.builder()
                .id(e.getId())
                .bookingCode(e.getBookingCode())
                .userId(e.getUser() != null ? e.getUser().getId() : null)
                .userFullName(e.getUser() != null ? e.getUser().getFullName() : null)
                .roomId(e.getRoom() != null ? e.getRoom().getId() : null)
                .roomName(e.getRoom() != null ? e.getRoom().getName() : null)
                .roomCode(e.getRoom() != null ? e.getRoom().getCode() : null)
                .checkinDate(e.getCheckinDate())
                .checkoutDate(e.getCheckoutDate())
                .guestsCount(e.getGuestsCount())
                .status(e.getStatus())
                .holdExpiresAt(e.getHoldExpiresAt())
                .totalPrice(e.getTotalPrice())
                .currency(e.getCurrency())
                .voucherCode(e.getVoucher() != null ? e.getVoucher().getCode() : null)
                .loyaltyDiscountPercent(e.getLoyaltyDiscountPercent())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
