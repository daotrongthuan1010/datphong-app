package com.vivu.booking.service;

import com.vivu.booking.dto.response.RevenuePointResponse;
import com.vivu.booking.dto.response.RevenueSummaryResponse;
import com.vivu.booking.dto.response.TopRoomResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Doanh thu — goc nhin nghiep vu (khong viet SQL, chi chon nguon doc).
 *
 * <p>Service quyet dinh: granularity DAY doc {@code v_revenue_daily} (VIEW luc nao cung dung),
 * MONTH doc {@code mv_revenue_monthly} (MATVIEW nhanh nhung co the tre toi thieu bang chu ky
 * cua {@code RevenueRefreshScheduler}). Log hi tam de nguoi hoc hieu cach dung dung loai view.
 */
public interface RevenueService {

    RevenueSummaryResponse overview(LocalDate from, LocalDate to, String granularity);

    List<RevenuePointResponse> series(LocalDate from, LocalDate to, String granularity);

    List<TopRoomResponse> topRooms(LocalDate from, LocalDate to, int limit);

    List<Map<String, Object>> occupancy(LocalDate from, LocalDate to);

    /** Lam moi matview — can quyen SYSTEM_CONFIG (admin). */
    void refreshMatView();

    String matViewLastRefresh();
}
