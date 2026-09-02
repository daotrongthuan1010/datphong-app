package com.vivu.booking.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RevenueReportResponse {

    private LocalDate fromDate;
    private LocalDate toDate;
    private String currency;

    private BigDecimal totalRevenue;
    private BigDecimal totalCommission;

    private Long totalBookings;
    private Long completedBookings;
    private Long cancelledBookings;

    private Long newUsers;
    private Long newHosts;

    private List<MonthlyRevenue> monthlyBreakdown;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MonthlyRevenue {
        /** Định dạng "yyyy-MM" */
        private String month;
        private BigDecimal revenue;
        private BigDecimal commission;
        private Long bookingsCount;
    }
}
