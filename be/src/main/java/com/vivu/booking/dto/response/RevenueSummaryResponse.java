package com.vivu.booking.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * KPI tong quan doanh thu — lay tu {@code fn_revenue_summary} trong 1 phat query.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RevenueSummaryResponse {
    private String from;
    private String to;
    private String currency;
    private BigDecimal grossRevenue;
    private BigDecimal commission;
    private BigDecimal netRevenue;
    private long bookings;
    private long nightsSold;
    private BigDecimal avgOrderValue;
    /** 0..1 (VD 0.07 = huy 7%). */
    private BigDecimal cancellationRate;
    private BigDecimal refundAmount;

    /** Cac diem tren bieu do: 1 diem = 1 ngay/tuan/thang. */
    private List<RevenuePointResponse> points;

    /** Thong tin cache/matview de FE hien "Nguon: ..." */
    private String source;
    /** ISO-8601 luc lam moi matview gan nhat (co the null). */
    private String matViewLastRefresh;
}
