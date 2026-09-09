package com.vivu.booking.controller;

import com.vivu.booking.dto.response.RevenuePointResponse;
import com.vivu.booking.dto.response.RevenueSummaryResponse;
import com.vivu.booking.dto.response.TopRoomResponse;
import com.vivu.booking.exception.BusinessException;
import com.vivu.booking.service.RevenueService;
import com.vivu.booking.service.impl.RevenueServiceImpl;
import com.vivu.booking.utils.JwtUtil;
import com.vivu.booking.utils.ServletUtils;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Bao cao doanh thu — mount tai {@code /api/admin/stats/*} (tuong tu /api/auth/* gom nhieu pathCon).
 *
 * <pre>
 * GET  /api/admin/stats/overview?from=YYYY-MM-DD&to=YYYY-MM-DD&granularity=DAY|WEEK|MONTH  — KPI + points
 * GET  /api/admin/stats/series?...         — chi diem bieu do
 * GET  /api/admin/stats/top-rooms?from=&to=&limit=         — top phong
 * GET  /api/admin/stats/occupancy?from=&to=                 — ty le lap day
 * GET  /api/admin/stats/sources                             — thong tin nguon (matview last refresh)
 * POST /api/admin/stats/refresh                             — REFRESH matview thu cong (admin)
 * </pre>
 *
 * <p>Phan quyen: GET can {@code REPORT_READ}, POST refresh can {@code SYSTEM_CONFIG} — de
 * {@link com.vivu.booking.filter.AuthorFilter} lo. Day khong phai authorisation sec-level.
 */
@WebServlet(urlPatterns = "/api/admin/stats/*")
public class AdminStatsServlet extends HttpServlet {

    private RevenueService revenueService;

    @Override public void init() { this.revenueService = new RevenueServiceImpl(); }
    public void setRevenueService(RevenueService svc) { this.revenueService = svc; }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String path = req.getPathInfo();
            if (path == null) path = "/";
            switch (path) {
                case "/overview" -> handleOverview(req, resp);
                case "/series" -> handleSeries(req, resp);
                case "/top-rooms" -> handleTopRooms(req, resp);
                case "/occupancy" -> handleOccupancy(req, resp);
                case "/sources" -> handleSources(req, resp);
                case "/", "" -> ServletUtils.writeJson(resp, 404,
                        Map.of("success", false, "message",
                                "GET /api/admin/stats/* — thu /overview, /series, /top-rooms, /occupancy, /sources"));
                default -> ServletUtils.writeJson(resp, 404,
                        Map.of("success", false, "message", "Khong tim thay endpoint: /api/admin/stats" + path));
            }
        } catch (Exception e) { ServletUtils.handleException(req, resp, e); }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String path = req.getPathInfo();
            if ("/refresh".equals(path)) {
                revenueService.refreshMatView();
                String last = revenueService.matViewLastRefresh();
                ServletUtils.ok(req, resp, Map.of("refreshed", true, "matViewLastRefresh", last == null ? "" : last));
                return;
            }
            ServletUtils.writeJson(resp, 404,
                    Map.of("success", false, "message", "Khong tim thay endpoint POST /api/admin/stats" + path));
        } catch (Exception e) { ServletUtils.handleException(req, resp, e); }
    }

    private void handleOverview(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        LocalDate[] range = range(req);
        String g = req.getParameter("granularity");
        RevenueSummaryResponse r = revenueService.overview(range[0], range[1], g);
        ServletUtils.ok(req, resp, r);
    }

    private void handleSeries(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        LocalDate[] range = range(req);
        String g = req.getParameter("granularity");
        List<RevenuePointResponse> pts = revenueService.series(range[0], range[1], g);
        ServletUtils.ok(req, resp, pts);
    }

    private void handleTopRooms(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        LocalDate[] range = range(req);
        int limit = ServletUtils.parseIntParam(req, "limit", 10);
        List<TopRoomResponse> rows = revenueService.topRooms(range[0], range[1], limit);
        ServletUtils.ok(req, resp, rows);
    }

    private void handleOccupancy(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        LocalDate[] range = range(req);
        ServletUtils.ok(req, resp, revenueService.occupancy(range[0], range[1]));
    }

    private void handleSources(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String last = revenueService.matViewLastRefresh();
        ServletUtils.ok(req, resp, Map.of(
                "sources", Map.of(
                        "view_daily", "v_revenue_daily (VIEW — luon dung)",
                        "matview_monthly", "mv_revenue_monthly (MATERIALIZED VIEW — refresh moi 15 phut)",
                        "matViewLastRefresh", last == null ? "" : last,
                        "functions", List.of("fn_revenue_summary", "fn_top_rooms", "fn_occupancy", "fn_refresh_revenue_mv()"))));
    }

    /** Macro ?period=7d|30d|90d|1y hoac pair ?from=&to=, uu tien from/to. */
    private static LocalDate[] range(HttpServletRequest req) {
        String from = req.getParameter("from");
        String to = req.getParameter("to");
        if (from != null && !from.isBlank() && to != null && !to.isBlank()) {
            return new LocalDate[]{LocalDate.parse(from.trim()), LocalDate.parse(to.trim())};
        }
        String period = req.getParameter("period");
        if ("7d".equalsIgnoreCase(period)) return new LocalDate[]{LocalDate.now().minusDays(7), LocalDate.now()};
        if ("90d".equalsIgnoreCase(period)) return new LocalDate[]{LocalDate.now().minusDays(90), LocalDate.now()};
        if ("1y".equalsIgnoreCase(period)) return new LocalDate[]{LocalDate.now().minusDays(365), LocalDate.now()};
        if (from != null || to != null) throw new BusinessException(400, "from/to phai di kem nhau (hoac dung ?period=7d|30d|90d|1y)");
        // Mac dinh 30 ngay — khong ra lai so lieu cu hay tra het DB
        return new LocalDate[]{LocalDate.now().minusDays(30), LocalDate.now()};
    }

    // Copy kieu requireAuth cua BookingServlet/PaymentServlet — sua auth deu doc nhu nhau.
    @SuppressWarnings("unused")
    private Long requireAuth(HttpServletRequest req) {
        String header = req.getHeader("Authorization");
        if (header != null && header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            String token = header.substring(7).trim();
            JwtUtil.Claims claims = JwtUtil.parse(token);
            if (!"access".equals(claims.getType()))
                throw new BusinessException(401, "Token khong phai access token");
            return claims.getUserId();
        }
        HttpSession sess = req.getSession(false);
        if (sess != null) {
            Object a = sess.getAttribute("user");
            if (a instanceof com.vivu.booking.dto.response.AuthTokenResponse.UserSummary us && us.getId() != null)
                return us.getId();
            if (a instanceof com.vivu.booking.dto.response.UsersLoginResponse ul && ul.getId() != null)
                return ul.getId();
        }
        throw new BusinessException(401, "Chua dang nhap — gui Authorization: Bearer <accessToken>");
    }
}
