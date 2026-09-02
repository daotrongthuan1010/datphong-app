package com.vivu.booking.mapper;

import com.vivu.booking.dto.response.RevenueReportResponse;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AdminReportMapper {

    public static RevenueReportResponse toRevenueReport(
            LocalDate fromDate,
            LocalDate toDate,
            String currency,
            BigDecimal totalRevenue,
            BigDecimal totalCommission,
            Long totalBookings,
            Long completedBookings,
            Long cancelledBookings,
            Long newUsers,
            Long newHosts,
            List<RevenueReportResponse.MonthlyRevenue> monthlyBreakdown) {

        return RevenueReportResponse.builder()
                .fromDate(fromDate)
                .toDate(toDate)
                .currency(currency)
                .totalRevenue(totalRevenue != null ? totalRevenue : BigDecimal.ZERO)
                .totalCommission(totalCommission != null ? totalCommission : BigDecimal.ZERO)
                .totalBookings(totalBookings != null ? totalBookings : 0L)
                .completedBookings(completedBookings != null ? completedBookings : 0L)
                .cancelledBookings(cancelledBookings != null ? cancelledBookings : 0L)
                .newUsers(newUsers != null ? newUsers : 0L)
                .newHosts(newHosts != null ? newHosts : 0L)
                .monthlyBreakdown(monthlyBreakdown)
                .build();
    }
}
