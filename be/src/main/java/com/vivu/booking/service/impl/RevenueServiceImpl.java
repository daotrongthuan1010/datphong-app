package com.vivu.booking.service.impl;

import com.vivu.booking.config.RedisConfig;
import com.vivu.booking.dao.ReportDao;
import com.vivu.booking.dto.response.RevenuePointResponse;
import com.vivu.booking.dto.response.RevenueSummaryResponse;
import com.vivu.booking.dto.response.TopRoomResponse;
import com.vivu.booking.exception.BusinessException;
import com.vivu.booking.mapper.RevenueMapper;
import com.vivu.booking.service.RevenueService;
import com.vivu.booking.utils.AppProperties;
import com.vivu.booking.utils.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Nghiep vu bao cao doanh thu.
 *
 * <h2>Service lam gi, va KHONG lam gi</h2>
 * <p>
 * Service <b>khong viet</b> SUM/JOIN. Toan bo dinh nghia "doanh thu la gi" nam trong
 * {@code resources/db/revenue-objects.sql} (VIEW / MATERIALIZED VIEW / FUNCTION). Service chi
 * quyet dinh <b>doc tu tang nao</b>, cache ket qua, va doi loi thanh thong bao hieu duoc.
 * Tach nhu vay de khi doi cong thuc tinh hoa hong thi sua 1 cho trong SQL, khong phai
 * sua 3 cho trong Java.
 *
 * <h2>VIEW hay MATERIALIZED VIEW?</h2>
 * <table border="1">
 *   <tr><th>Do phan giai</th><th>Nguon doc</th><th>Doi lai</th></tr>
 *   <tr><td>DAY, WEEK</td><td>{@code v_revenue_daily} (VIEW)</td>
 *       <td>Tinh lai moi lan doc — cham hon khi du lieu lon, nhung luon khop so lieu that</td></tr>
 *   <tr><td>MONTH</td><td>{@code mv_revenue_monthly} (MATERIALIZED VIEW)</td>
 *       <td>Doc ban sao vat ly rat nhanh, doi lai tre toi da = chu ky cua
 *           {@code RevenueRefreshScheduler}</td></tr>
 * </table>
 * Bieu do 12 thang ma doc VIEW thi moi lan mo dashboard phai khai trien lai toan bo
 * {@code booking x dem} — mat vai giay khi du lieu lon. Do la ly do ton tai matview.
 *
 * <h2>Redis cache</h2>
 * Key {@code stats:revenue:{sha256(params)}} TTL {@code report.cache-ttl-seconds} (60s).
 * Redis chet thi <b>chi log roi bo qua</b> — dashboard van chay bang query truc tiep,
 * khong duoc bien cache thanh single point of failure (dung nguyen tac voi
 * {@code RedisLockUtil}: Redis de giam tai, khong phai de quyet dinh dung/sai).
 *
 * <h2>Khi DB objects chua ton tai</h2>
 * Tra <b>503 kem thong bao ro</b>, khong tra ve 0. Bieu do doanh thu hien 0 dong trong khi
 * that ra co tien la loi nguy hiem hon nhieu so voi mot man hinh bao loi.
 */
public class RevenueServiceImpl implements RevenueService {

    private static final Logger log = LoggerFactory.getLogger(RevenueServiceImpl.class);

    private static final int CACHE_TTL_SECONDS = AppProperties.getInt("report.cache-ttl-seconds", 60);
    private static final String CACHE_PREFIX = "stats:revenue:";
    private static final Set<String> GRANULARITIES = Set.of("DAY", "WEEK", "MONTH");
    /** Tuan ISO: bat dau thu Hai, tuan dau tien phai co it nhat 4 ngay. */
    private static final WeekFields WEEK_FIELDS = WeekFields.of(DayOfWeek.MONDAY, 4);

    private final ReportDao reportDao;

    public RevenueServiceImpl(ReportDao reportDao) {
        this.reportDao = reportDao;
    }

    public RevenueServiceImpl() {
        this(new ReportDao());
    }

    @Override
    public RevenueSummaryResponse overview(LocalDate from, LocalDate to, String granularity) {
        validateRange(from, to);
        String g = normalizeGranularity(granularity);
        requireReportObjects();

        Map<String, Object> kpi = reportDao.revenueSummary(from, to);
        List<RevenuePointResponse> points = seriesInternal(from, to, g);

        String source = "MONTH".equals(g)
                ? "mv_revenue_monthly (MATERIALIZED VIEW — lam moi moi "
                        + AppProperties.getInt("report.refresh-interval-minutes", 15) + " phut)"
                : "v_revenue_daily (VIEW — tinh lai theo du lieu moi nhat)";

        return RevenueMapper.toSummary(kpi, points, source, reportDao.matViewLastRefresh(),
                from.toString(), to.toString());
    }

    @Override
    public List<RevenuePointResponse> series(LocalDate from, LocalDate to, String granularity) {
        validateRange(from, to);
        requireReportObjects();
        return seriesInternal(from, to, normalizeGranularity(granularity));
    }

    @Override
    public List<TopRoomResponse> topRooms(LocalDate from, LocalDate to, int limit) {
        validateRange(from, to);
        requireReportObjects();
        int n = Math.min(Math.max(limit, 1), 50);
        return reportDao.topRooms(from, to, n).stream().map(RevenueMapper::toTop).toList();
    }

    @Override
    public List<Map<String, Object>> occupancy(LocalDate from, LocalDate to) {
        validateRange(from, to);
        requireReportObjects();
        return reportDao.occupancy(from, to);
    }

    @Override
    public void refreshMatView() {
        reportDao.refreshMonthlyMatView();
        evictCache();
    }

    @Override
    public String matViewLastRefresh() {
        return reportDao.matViewLastRefresh();
    }

    // ==================================================================== helpers

    /**
     * Doc chuoi diem theo do phan giai.
     *
     * <p>WEEK khong co object DB rieng: doc VIEW theo ngay roi rollup sang tuan ISO o Java.
     * Ly do khong lam them mot view tuan: tuan chi la gop nhom cua ngay, them view nua la
     * them mot cho co the lech dinh nghia.
     */
    private List<RevenuePointResponse> seriesInternal(LocalDate from, LocalDate to, String g) {
        String cacheKey = cacheKey("series:" + g + ":" + from + ":" + to);
        String cached = cacheGet(cacheKey);
        if (cached != null) {
            List<RevenuePointResponse> hits = JsonUtils.fromJsonList(cached, RevenuePointResponse.class);
            if (hits != null) {
                log.debug("Revenue cache hit {}", cacheKey);
                return hits;
            }
        }

        List<Map<String, Object>> raw;
        try {
            if ("MONTH".equals(g)) {
                raw = reportDao.monthlyRevenue(from, to);
            } else {
                raw = reportDao.dailyRevenue(from, to);
                if ("WEEK".equals(g)) raw = rollupByWeek(raw);
            }
        } catch (RuntimeException e) {
            throw translateDbError(e);
        }

        List<RevenuePointResponse> out = raw.stream().map(RevenueMapper::toPoint).toList();
        cachePut(cacheKey, JsonUtils.toJson(out));
        return out;
    }

    /**
     * Gom cac diem ngay thanh tuan (thu Hai la ngay dau tuan).
     *
     * <p>Dung {@link LinkedHashMap} de giu thu tu thoi gian — bieu do ve theo thu tu xuat hien,
     * mat thu tu thi duong ve bi vong lung tung.
     */
    private static List<Map<String, Object>> rollupByWeek(List<Map<String, Object>> daily) {
        Map<String, Map<String, Object>> byWeek = new LinkedHashMap<>();
        for (Map<String, Object> day : daily) {
            LocalDate d;
            try {
                d = LocalDate.parse(String.valueOf(day.get("period")));
            } catch (Exception e) {
                continue; // dong la (ky tu trong) — bo qua hon la lam chet ca bieu do
            }
            String weekStart = d.with(WEEK_FIELDS.dayOfWeek(), 1L).toString();
            Map<String, Object> acc = byWeek.computeIfAbsent(weekStart, k -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("period", k);
                m.put("revenue", BigDecimal.ZERO);
                m.put("commission", BigDecimal.ZERO);
                m.put("bookings", 0L);
                m.put("nights", 0L);
                return m;
            });
            acc.put("revenue", dec(acc.get("revenue")).add(dec(day.get("revenue"))));
            acc.put("commission", dec(acc.get("commission")).add(dec(day.get("commission"))));
            acc.put("bookings", lng(acc.get("bookings")) + lng(day.get("bookings")));
            acc.put("nights", lng(acc.get("nights")) + lng(day.get("nights")));
        }
        return new ArrayList<>(byWeek.values());
    }

    /**
     * Doi loi SQL thanh thong bao nguoi dung doc hieu.
     *
     * <p>Nguyen nhan pho bien nhat: {@code SqlScriptRunner} khong tao duoc view/function
     * (DB user thieu quyen CREATE, hoac start lan dau khi bang chua ton tai). Neu de nguyen
     * loi "relation v_revenue_daily does not exist" lot ra thi thanh 500 Internal Server Error
     * — nguoi xem khong biet phai lam gi.
     */
    private static BusinessException translateDbError(RuntimeException e) {
        String msg = String.valueOf(e.getMessage());
        String lower = msg.toLowerCase();
        if (lower.contains("does not exist") || lower.contains("not exist")) {
            return new BusinessException(503,
                    "Bao cao chua san sang: doi tuong DB (view/function) chua duoc tao — "
                            + "kiem tra log SqlScriptRunner va quyen CREATE cua user DB. Chi tiet: " + msg);
        }
        throw e;
    }

    private void requireReportObjects() {
        if (!reportDao.reportObjectsReady()) {
            throw new BusinessException(503,
                    "Bao cao chua san sang: v_revenue_daily / mv_revenue_monthly chua ton tai trong DB. "
                            + "App tu chay /db/revenue-objects.sql luc khoi dong — thu lai sau vai giay "
                            + "hoac kiem tra quyen CREATE cua user DB.");
        }
    }

    private static void validateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new BusinessException(400, "Thieu from/to (dinh dang YYYY-MM-DD) hoac ?period=7d|30d|90d|1y");
        }
        if (from.isAfter(to)) {
            throw new BusinessException(400, "from phai truoc hoac bang to");
        }
        // Chanh query generate_series hang chuc nghin ngay khi ai do go from=1970-01-01
        if (from.plusYears(5).isBefore(to)) {
            throw new BusinessException(400, "Khoang thoi gian qua rong (toi da 5 nam)");
        }
    }

    private static String normalizeGranularity(String g) {
        if (g == null || g.isBlank()) return "DAY";
        String u = g.trim().toUpperCase();
        if (!GRANULARITIES.contains(u)) {
            throw new BusinessException(400, "granularity phai la DAY / WEEK / MONTH, nhan duoc: " + g);
        }
        return u;
    }

    // ---------------------------------------------------------------- Redis cache

    private static String cacheGet(String key) {
        try (Jedis j = RedisConfig.getPool().getResource()) {
            return j.get(key);
        } catch (Exception e) {
            log.debug("Redis cache read fail (bo qua, query truc tiep): {}", e.getMessage());
            return null;
        }
    }

    private static void cachePut(String key, String json) {
        try (Jedis j = RedisConfig.getPool().getResource()) {
            j.setex(key, CACHE_TTL_SECONDS, json);
        } catch (Exception e) {
            log.debug("Redis cache write fail (bo qua): {}", e.getMessage());
        }
    }

    /**
     * Xoa cache sau khi REFRESH matview — neu khong thi nguoi dung bam "Lam moi du lieu"
     * xong van thay so cu trong 60 giay va tuong nut khong chay.
     */
    private static void evictCache() {
        try (Jedis j = RedisConfig.getPool().getResource()) {
            var keys = j.keys(CACHE_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                j.del(keys.toArray(new String[0]));
                log.info("Da xoa {} key cache bao cao sau khi refresh matview", keys.size());
            }
        } catch (Exception e) {
            log.debug("Khong xoa duoc cache bao cao: {}", e.getMessage());
        }
    }

    private static String cacheKey(String raw) {
        try {
            byte[] h = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(CACHE_PREFIX);
            for (byte b : h) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return CACHE_PREFIX + raw.hashCode();
        }
    }

    private static BigDecimal dec(Object o) {
        if (o == null) return BigDecimal.ZERO;
        if (o instanceof BigDecimal b) return b;
        if (o instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        try {
            return new BigDecimal(String.valueOf(o));
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private static long lng(Object o) {
        if (o == null) return 0L;
        if (o instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(String.valueOf(o));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
