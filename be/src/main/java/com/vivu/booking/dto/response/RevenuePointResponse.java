package com.vivu.booking.dto.response;

import lombok.*;

import java.math.BigDecimal;

/** 1 diem tren bieu do doanh thu (ngay/tuan/thang). */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RevenuePointResponse {
    private String period;
    private BigDecimal revenue;
    private BigDecimal commission;
    private long bookings;
    private long nights;
    private String roomType;
    private BigDecimal occupancyRate;
}
