package com.vivu.booking.dao;

import com.vivu.booking.config.HibernateConfig;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * DAO bao cao doanh thu — khong co {@code @Entity} rieng, chi doc cac doi tuong ma
 * {@code resources/db/revenue-objects.sql} tao ra (VIEW / MATERIALIZED VIEW / FUNCTION).
 *
 * <h2>Vì sao khong extends BaseDao</h2>
 * {@link BaseDao} gan voi mot entity co {@code @Id} de load/merge/delete. Ket qua bao cao la
 * cac dong <b>tinh toan</b> (SUM, COUNT, ROUND) — khong co ban ghi goc nao de quan ly vong doi.
 * Vi vay DAO nay mo Session truc tiep qua {@link HibernateConfig}, giong cach
 * {@code AuthorizationService} doc {@code role_permissions}.
 *
 * <h2>Vì sao toan bo aggregate nam trong DB, khong nam o day</h2>
 * Neu de Java tu viet SUM/JOIN thi "doanh thu" se co nhieu dinh nghia song song: mot ban trong
 * dashboard, mot ban trong job, mot ban trong export. Chung lech nhau la chuyen som muon, va
 * khi lech thi khong ai biet ban nao dung. Day xuong VIEW/FUNCTION thi chi co MOT dinh nghia.
 *
 * <h2>Ep kieu JDBC</h2>
 * {@code numeric} ve {@link BigDecimal}, {@code bigint} ve {@link Long}, {@code date} ve
 * {@link java.sql.Date}. Hai ham {@code dec()}/{@code lng()} gom viec ep kieu ve mot cho.
 * Dung {@link LinkedHashMap} (khong phai {@code Map.of}) vi {@code Map.of} nem NPE khi co
 * gia tri null — ma {@code room_type} hay {@code last_refresh} hoan toan co the null.
 */
public class ReportDao {

    private static final Logger log = LoggerFactory.getLogger(ReportDao.class);

    private <R> R read(Function<Session, R> work) {
        try (Session s = HibernateConfig.getSessionFactory().openSession()) {
            return work.apply(s);
        }
    }

    /**
     * Toan bo KPI trong MOT phat query — {@code SELECT * FROM fn_revenue_summary(from, to)}.
     *
     * <p>Mot function tra 10 cot thay vi 10 query rieng: it round-trip hon, va quan trong hon
     * la moi chi so duoc tinh tren cung mot snapshot du lieu (10 query rieng co the doc
     * 10 trang thai khac nhau neu co ghi xen giua).
     */
    public Map<String, Object> revenueSummary(LocalDate from, LocalDate to) {
        return read(s -> {
            Object[] r = (Object[]) s.createNativeQuery("SELECT * FROM fn_revenue_summary(:from, :to)")
                    .setParameter("from", from)
                    .setParameter("to", to)
                    .uniqueResult();
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("grossRevenue", dec(r[0]));
            m.put("commission", dec(r[1]));
            m.put("netRevenue", dec(r[2]));
            m.put("refundAmount", dec(r[3]));
            m.put("bookings", lng(r[4]));
            m.put("bookingsCreated", lng(r[5]));
            m.put("cancelledBookings", lng(r[6]));
            m.put("cancellationRate", dec(r[7]));
            m.put("nightsSold", lng(r[8]));
            m.put("avgOrderValue", dec(r[9]));
            return m;
        });
    }

    /**
     * Doanh thu theo ngay — doc {@code v_revenue_daily} (VIEW thuong).
     *
     * <p>VIEW tinh lai moi lan doc nen luon khop du lieu hien tai. Do la ly do dung no cho
     * do phan giai DAY/WEEK (khoang ngan, it dong), con MONTH thi dung matview.
     */
    public List<Map<String, Object>> dailyRevenue(LocalDate from, LocalDate to) {
        return read(s -> {
            @SuppressWarnings("unchecked")
            List<Object[]> rows = s.createNativeQuery("""
                            SELECT revenue_date, gross_revenue, commission, bookings_count, nights_sold
                            FROM v_revenue_daily
                            WHERE revenue_date BETWEEN :from AND :to
                            ORDER BY revenue_date
                            """)
                    .setParameter("from", from)
                    .setParameter("to", to)
                    .getResultList();
            List<Map<String, Object>> out = new ArrayList<>(rows.size());
            for (Object[] r : rows) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("period", String.valueOf(r[0]));
                m.put("revenue", dec(r[1]));
                m.put("commission", dec(r[2]));
                m.put("bookings", lng(r[3]));
                m.put("nights", lng(r[4]));
                out.add(m);
            }
            return out;
        });
    }

    /**
     * Doanh thu theo thang x loai phong — doc {@code mv_revenue_monthly} (MATERIALIZED VIEW).
     *
     * <p>Khac {@link #dailyRevenue} o cho day la <b>ban sao vat ly</b>: doc chi vai chuc dong
     * da tong hop san nen bieu do 12 thang mo tuc thi. Doi lai du lieu co the tre toi da
     * bang chu ky cua {@code RevenueRefreshScheduler} — danh doi co chu y, va FE hien ro
     * thoi diem cap nhat cuoi de nguoi xem khong nghi so lieu la thoi gian thuc.
     */
    public List<Map<String, Object>> monthlyRevenue(LocalDate from, LocalDate to) {
        return read(s -> {
            @SuppressWarnings("unchecked")
            List<Object[]> rows = s.createNativeQuery("""
                            SELECT revenue_month, room_type, gross_revenue, commission, bookings_count, nights_sold
                            FROM mv_revenue_monthly
                            WHERE revenue_month BETWEEN date_trunc('month', :from)::date
                                                    AND date_trunc('month', :to)::date
                            ORDER BY revenue_month, gross_revenue DESC
                            """)
                    .setParameter("from", from)
                    .setParameter("to", to)
                    .getResultList();
            List<Map<String, Object>> out = new ArrayList<>(rows.size());
            for (Object[] r : rows) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("period", String.valueOf(r[0]));
                m.put("roomType", r[1] == null ? "" : String.valueOf(r[1]));
                m.put("revenue", dec(r[2]));
                m.put("commission", dec(r[3]));
                m.put("bookings", lng(r[4]));
                m.put("nights", lng(r[5]));
                out.add(m);
            }
            return out;
        });
    }

    /** Top phong theo doanh thu — {@code fn_top_rooms(from, to, limit)}. */
    public List<Map<String, Object>> topRooms(LocalDate from, LocalDate to, int limit) {
        return read(s -> {
            @SuppressWarnings("unchecked")
            List<Object[]> rows = s.createNativeQuery("SELECT * FROM fn_top_rooms(:from, :to, :lim)")
                    .setParameter("from", from)
                    .setParameter("to", to)
                    .setParameter("lim", limit)
                    .getResultList();
            List<Map<String, Object>> out = new ArrayList<>(rows.size());
            for (Object[] r : rows) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("roomId", lng(r[0]));
                m.put("roomCode", str(r[1]));
                m.put("roomName", str(r[2]));
                m.put("roomType", str(r[3]));
                m.put("revenue", dec(r[4]));
                m.put("bookings", lng(r[5]));
                m.put("nights", lng(r[6]));
                m.put("rank", lng(r[7]));
                out.add(m);
            }
            return out;
        });
    }

    /** Cong suat phong theo ngay — {@code fn_occupancy(from, to)}. */
    public List<Map<String, Object>> occupancy(LocalDate from, LocalDate to) {
        return read(s -> {
            @SuppressWarnings("unchecked")
            List<Object[]> rows = s.createNativeQuery("SELECT * FROM fn_occupancy(:from, :to)")
                    .setParameter("from", from)
                    .setParameter("to", to)
                    .getResultList();
            List<Map<String, Object>> out = new ArrayList<>(rows.size());
            for (Object[] r : rows) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("period", String.valueOf(r[0]));
                m.put("roomsOccupied", lng(r[1]));
                m.put("roomsBlocked", lng(r[2]));
                m.put("roomsTotal", lng(r[3]));
                m.put("occupancyRate", dec(r[4]));
                m.put("blockRate", dec(r[5]));
                out.add(m);
            }
            return out;
        });
    }

    /**
     * Lam moi matview — {@code CALL fn_refresh_revenue_mv()}.
     *
     * <p>Phai la PROCEDURE goi bang {@code CALL}: {@code REFRESH MATERIALIZED VIEW} la lenh ghi,
     * khong dat trong FUNCTION SQL thuan duoc. Ben trong procedure da co
     * {@code CONCURRENTLY} kem fallback sang REFRESH thuong neu khong thoa dieu kien unique index.
     *
     * <p>Dung {@code doWork} (JDBC thuan) thay vi {@code createNativeQuery().executeUpdate()}:
     * Hibernate map {@code CALL} sang {@code executeUpdate} khong on dinh giua cac ban.
     */
    public void refreshMonthlyMatView() {
        try (Session s = HibernateConfig.getSessionFactory().openSession()) {
            var tx = s.beginTransaction();
            try {
                s.doWork(conn -> {
                    try (var st = conn.createStatement()) {
                        st.execute("CALL fn_refresh_revenue_mv()");
                    }
                });
                tx.commit();
                log.info("Da REFRESH mv_revenue_monthly");
            } catch (RuntimeException e) {
                if (tx.isActive()) tx.rollback();
                throw e;
            }
        }
    }

    /**
     * Matview duoc lam moi lan cuoi luc nao — FE hien "du lieu cap nhat luc ...".
     * Tra {@code null} khi chua tung refresh hoac stats collector chua ghi nhan.
     */
    public String matViewLastRefresh() {
        try {
            return read(s -> {
                Object o = s.createNativeQuery("""
                                SELECT to_char(max(last_refresh_end_time), 'YYYY-MM-DD HH24:MI:SS')
                                FROM pg_stat_user_tables
                                WHERE relname = 'mv_revenue_monthly'
                                """).uniqueResult();
                return o == null ? null : String.valueOf(o);
            });
        } catch (RuntimeException e) {
            log.debug("Khong doc duoc thoi diem refresh matview: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Kiem tra cac doi tuong bao cao da ton tai chua — dung de tra 503 ro rang thay vi
     * de loi "relation does not exist" lot ra thanh 500 kho hieu.
     */
    public boolean reportObjectsReady() {
        try {
            return read(s -> {
                Number n = (Number) s.createNativeQuery("""
                                SELECT count(*) FROM pg_class c
                                JOIN pg_namespace ns ON ns.oid = c.relnamespace
                                WHERE ns.nspname = 'public'
                                  AND c.relname IN ('v_revenue_daily','mv_revenue_monthly','v_booking_night')
                                """).uniqueResult();
                return n != null && n.intValue() >= 3;
            });
        } catch (RuntimeException e) {
            return false;
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

    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o);
    }
}
