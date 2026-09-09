package com.vivu.booking.mapper;

import com.vivu.booking.dto.response.RevenueSummaryResponse;
import com.vivu.booking.dto.response.RevenuePointResponse;
import com.vivu.booking.dto.response.TopRoomResponse;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RevenueMapper {

    public static RevenueSummaryResponse toSummary(Map<String, Object> m, List<RevenuePointResponse> points,
                                                   String source, String matViewLastRefresh,
                                                   String from, String to) {
        return RevenueSummaryResponse.builder()
                .from(from).to(to).currency("VND")
                .grossRevenue(nz(m.get("grossRevenue")))
                .commission(nz(m.get("commission")))
                .netRevenue(nz(m.get("netRevenue")))
                .bookings(lng(m.get("bookings")))
                .nightsSold(lng(m.get("nightsSold")))
                .avgOrderValue(nz(m.get("avgOrderValue")))
                .cancellationRate(nz(m.get("cancellationRate")))
                .refundAmount(nz(m.get("refundAmount")))
                .points(points)
                .source(source)
                .matViewLastRefresh(matViewLastRefresh)
                .build();
    }

    public static RevenuePointResponse toPoint(Map<String, Object> m) {
        return RevenuePointResponse.builder()
                .period(String.valueOf(m.getOrDefault("period", "")))
                .revenue(nz(m.get("revenue")))
                .commission(nz(m.get("commission")))
                .bookings(lng(m.get("bookings")))
                .nights(lng(m.getOrDefault("nights", m.get("nightsSold"))))
                .roomType(m.get("roomType") == null ? null : String.valueOf(m.get("roomType")))
                .occupancyRate(m.get("occupancyRate") == null ? null : nz(m.get("occupancyRate")))
                .build();
    }

    public static TopRoomResponse toTop(Map<String, Object> m) {
        return TopRoomResponse.builder()
                .roomId(lngBox(m.get("roomId")))
                .roomCode(String.valueOf(m.getOrDefault("roomCode", "")))
                .roomName(String.valueOf(m.getOrDefault("roomName", "")))
                .revenue(nz(m.get("revenue")))
                .bookings(lng(m.get("bookings")))
                .nights(lng(m.get("nights")))
                .build();
    }

    private static BigDecimal nz(Object o) {
        if (o == null) return BigDecimal.ZERO;
        if (o instanceof BigDecimal b) return b;
        return new BigDecimal(String.valueOf(o));
    }

    private static long lng(Object o) {
        if (o == null) return 0L;
        if (o instanceof Number n) return n.longValue();
        return Long.parseLong(String.valueOf(o));
    }

    private static Long lngBox(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.longValue();
        return Long.parseLong(String.valueOf(o));
    }
}
