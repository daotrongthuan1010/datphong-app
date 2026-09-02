package com.vivu.booking.dto.response;

import com.vivu.booking.enums.BookingStatusType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingResponse {
    private Long id;
    private String bookingCode;

    private Long userId;
    private String userFullName;

    private Long roomId;
    private String roomName;
    private String roomCode;

    private LocalDate checkinDate;
    private LocalDate checkoutDate;
    private Integer guestsCount;

    private BookingStatusType status;
    private LocalDateTime holdExpiresAt;

    private BigDecimal totalPrice;
    private String currency;

    private String voucherCode;
    private BigDecimal loyaltyDiscountPercent;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
