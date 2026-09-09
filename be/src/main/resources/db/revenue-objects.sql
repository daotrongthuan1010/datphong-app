-- =============================================================================
--  VIVU Booking — doi tuong DB cho bao cao doanh thu (PostgreSQL)
--
--  Chay idempotent luc app start boi com.vivu.booking.utils.SqlScriptRunner
--  (duoc goi tu AppContextListener.contextInitialized, SAU khi hbm2ddl=update
--  da tao xong cac bang). Tat ca deu OR REPLACE / IF NOT EXISTS / DROP+CREATE
--  de chay lai bao nhieu lan cung khong loi.
--
--  hbm2ddl.auto=update chi tao duoc BANG tu @Entity. No khong biet gi ve VIEW,
--  MATERIALIZED VIEW hay FUNCTION — vi vay can file nay.
--
--  BA TANG, MOI TANG MOT MUC DICH (day la phan dang hoc nhat):
--    1. VIEW thuong            = dinh nghia dung, tinh lai moi lan doc (cham voi du lieu lon)
--    2. MATERIALIZED VIEW      = ban sao vat ly, doc cuc nhanh, doi lai phai REFRESH
--    3. FUNCTION/PROCEDURE     = logic gong dung chung cho dashboard / export / job
--
--  HAI TRUC THOI GIAN KHAC NHAU — nham lan giua chung la loi pho bien nhat:
--    * Tien ve khi nao  -> payments.paid_at   (dung cho DOANH THU)
--    * Khach o khi nao  -> bookings.checkin/checkout_date (dung cho CONG SUAT PHONG)
--  Bieu do doanh thu ve theo paid_at. Bieu do lap day ve theo ngay luu tru.
--  Mot don dat thang 9 ma khach o thang 10 thi doanh thu tinh vao thang 9.
--
--  NGUON TIEN DUY NHAT la payments.amount voi status = 'SUCCESS'.
--  KHONG dung SUM(room_calendar.price_override) vi gia dem chua tru voucher;
--  cung KHONG dung bookings.total_price vi no la gia luc dat, khong phai tien da thu.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1) v_booking_night — VIEW: khai trien moi booking thanh 1 dong / 1 dem luu tru.
--    Dem thu i cua booking = checkin_date + i, i chay tu 0 toi (so dem - 1).
--    generate_series tra integer nen khong can lo lech thang/nam nhuan.
-- -----------------------------------------------------------------------------
CREATE OR REPLACE VIEW v_booking_night AS
SELECT
    b.id                                             AS booking_id,
    b.room_id,
    b.user_id,
    r.code                                           AS room_code,
    r.name                                           AS room_name,
    r.type                                           AS room_type,
    (b.checkin_date + gs.i)::date                    AS stay_date,
    date_trunc('month', (b.checkin_date + gs.i))::date AS stay_month,
    b.checkin_date,
    b.checkout_date,
    (b.checkout_date - b.checkin_date)               AS nights,
    b.total_price,
    b.status::text                                   AS booking_status,
    p.status::text                                   AS payment_status,
    p.amount                                         AS paid_amount,
    COALESCE(p.paid_at, p.created_at)                AS revenue_at,
    COALESCE(p.status::text = 'SUCCESS', false)      AS is_paid
FROM bookings b
JOIN rooms r ON r.id = b.room_id
LEFT JOIN payments p ON p.booking_id = b.id
CROSS JOIN LATERAL generate_series(
        0,
        GREATEST((b.checkout_date - b.checkin_date) - 1, 0)
     ) AS gs(i);

-- -----------------------------------------------------------------------------
-- 2) v_revenue_daily — VIEW: doanh thu theo NGAY TIEN VE.
--    Hoa hong lay tu system_settings.commission_percent (seed-data.sql dat 10),
--    fallback 10% neu bang rong. Doc tu settings thay vi hardcode de doi
--    chinh sach hoa hong khong phai sua SQL.
-- -----------------------------------------------------------------------------
CREATE OR REPLACE VIEW v_revenue_daily AS
WITH cfg AS (
    SELECT COALESCE(
               NULLIF(regexp_replace(setting_value, '[^0-9.]', '', 'g'), '')::numeric,
               10) AS commission_percent
    FROM system_settings
    WHERE setting_key = 'commission_percent'
    LIMIT 1
),
paid AS (
    SELECT
        n.revenue_at::date  AS revenue_date,
        n.booking_id,
        n.paid_amount,
        n.nights
    FROM v_booking_night n
    WHERE n.is_paid AND n.revenue_at IS NOT NULL
)
SELECT
    p.revenue_date,
    date_trunc('month', p.revenue_date)::date          AS revenue_month,
    COUNT(*)::bigint                                   AS payments_count,
    COUNT(DISTINCT p.booking_id)::bigint               AS bookings_count,
    SUM(p.paid_amount)                                 AS gross_revenue,
    SUM(p.paid_amount) * COALESCE(c.commission_percent, 10) / 100 AS commission,
    SUM(p.nights)::bigint                              AS nights_sold
FROM paid p
CROSS JOIN cfg c
GROUP BY p.revenue_date, c.commission_percent;

-- -----------------------------------------------------------------------------
-- 3) mv_revenue_monthly — MATERIALIZED VIEW: doanh thu theo THANG x LOAI PHONG.
--
--    Vi sao khong dung VIEW thuong: bieu do 12 thang phai quet lai toan bo
--    bookings x payments x generate_series moi lan mo dashboard. Vat ly hoa
--    thi chi doc vai chuc dong da tong hop. Doi lai: du lieu tre toi da bang
--    chu ky cua RevenueRefreshScheduler (15 phut, cau hinh
--    report.refresh-interval-minutes).
--
--    DROP + CREATE moi lan start de dinh nghia luon khop voi file nay
--    (IF NOT EXISTS se giu lai dinh nghia cu khi ta sua SQL). Du lieu duoc
--    nap lai ngay trong CREATE, chi mat vai giay voi du lieu hoc.
-- -----------------------------------------------------------------------------
DROP MATERIALIZED VIEW IF EXISTS mv_revenue_monthly;

CREATE MATERIALIZED VIEW mv_revenue_monthly AS
WITH cfg AS (
    SELECT COALESCE(
               NULLIF(regexp_replace(setting_value, '[^0-9.]', '', 'g'), '')::numeric,
               10) AS commission_percent
    FROM system_settings
    WHERE setting_key = 'commission_percent'
    LIMIT 1
),
paid AS (
    SELECT
        date_trunc('month', n.revenue_at)::date AS revenue_month,
        n.room_type,
        n.room_id,
        n.booking_id,
        n.paid_amount,
        n.nights
    FROM v_booking_night n
    WHERE n.is_paid AND n.revenue_at IS NOT NULL
)
SELECT
    p.revenue_month,
    p.room_type,
    COUNT(*)::bigint                                   AS payments_count,
    COUNT(DISTINCT p.booking_id)::bigint               AS bookings_count,
    SUM(p.paid_amount)                                 AS gross_revenue,
    SUM(p.paid_amount) * COALESCE(c.commission_percent, 10) / 100 AS commission,
    SUM(p.nights)::bigint                              AS nights_sold
FROM paid p
CROSS JOIN cfg c
GROUP BY p.revenue_month, p.room_type, c.commission_percent;

-- Unique index bat buoc cho REFRESH ... CONCURRENTLY (khong khoa bang khi doc).
CREATE UNIQUE INDEX IF NOT EXISTS ux_mv_revenue_monthly
    ON mv_revenue_monthly (revenue_month, room_type);

CREATE INDEX IF NOT EXISTS ix_mv_revenue_monthly_month
    ON mv_revenue_monthly (revenue_month);

-- -----------------------------------------------------------------------------
-- 4) fn_revenue_summary — FUNCTION: toan bo KPI trong MOT phat query.
--    Dashboard mo len = 1 round-trip thay vi 8 query rieng.
--
--    RETURNS TABLE de Hibernate doc ket qua nhu mot resultset binh thuong
--    (SELECT * FROM fn_revenue_summary(:from, :to)).
--    STABLE = huat nguyen ham khong ghi du lieu, planner duoc phep goi 1 lan.
-- -----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_revenue_summary(p_from date, p_to date)
RETURNS TABLE (
    gross_revenue     numeric,
    commission        numeric,
    net_revenue       numeric,
    refund_amount     numeric,
    paid_bookings     bigint,
    bookings_created  bigint,
    cancelled_bookings bigint,
    cancellation_rate numeric,
    nights_sold       bigint,
    avg_order_value   numeric
)
LANGUAGE sql
STABLE
AS $$
    WITH cfg AS (
        SELECT COALESCE(
                   NULLIF(regexp_replace(setting_value, '[^0-9.]', '', 'g'), '')::numeric,
                   10) AS commission_percent
        FROM system_settings
        WHERE setting_key = 'commission_percent'
        LIMIT 1
    ),
    -- Tien tinh theo NGAY THANH TOAN
    money AS (
        SELECT COALESCE(SUM(d.gross_revenue), 0) AS gross,
               COALESCE(SUM(d.commission), 0)    AS comm,
               COALESCE(SUM(d.bookings_count), 0) AS paid_bookings,
               COALESCE(SUM(d.nights_sold), 0)   AS nights
        FROM v_revenue_daily d
        WHERE d.revenue_date BETWEEN p_from AND p_to
    ),
    -- Don hang tinh theo NGAY TAO (co huy hay khong thi van la don da tao trong ky)
    cohort AS (
        SELECT COUNT(*)::bigint AS created,
               COUNT(*) FILTER (WHERE b.status::text IN ('CANCELLED', 'EXPIRED'))::bigint AS cancelled
        FROM bookings b
        WHERE b.created_at::date BETWEEN p_from AND p_to
    ),
    refunds AS (
        SELECT COALESCE(SUM(r.amount), 0) AS total
        FROM refund_requests r
        WHERE r.status::text = 'PAID'
          AND COALESCE(r.processed_at, r.created_at)::date BETWEEN p_from AND p_to
    )
    SELECT
        m.gross,
        m.comm,
        m.gross - m.comm - rf.total,
        rf.total,
        m.paid_bookings,
        c.created,
        c.cancelled,
        CASE WHEN c.created = 0 THEN 0
             ELSE ROUND(c.cancelled::numeric / c.created, 4) END,
        m.nights,
        CASE WHEN m.paid_bookings = 0 THEN 0
             ELSE ROUND(m.gross / m.paid_bookings, 2) END
    FROM money m
    CROSS JOIN cohort c
    CROSS JOIN refunds rf
    CROSS JOIN cfg
$$;

-- -----------------------------------------------------------------------------
-- 5) fn_top_rooms — FUNCTION: phong ban chay nhat theo doanh thu.
--    Dung CTE + ROW_NUMBER() OVER de xep hang — mau chuan khi muon "top N theo nhom".
-- -----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_top_rooms(p_from date, p_to date, p_limit int)
RETURNS TABLE (
    room_id     bigint,
    room_code   varchar,
    room_name   varchar,
    room_type   varchar,
    revenue     numeric,
    bookings    bigint,
    nights      bigint,
    rank_no     bigint
)
LANGUAGE sql
STABLE
AS $$
    WITH paid AS (
        SELECT n.room_id, n.room_code, n.room_name, n.room_type::text AS room_type,
               n.booking_id, n.paid_amount, n.nights
        FROM v_booking_night n
        WHERE n.is_paid
          AND n.revenue_at::date BETWEEN p_from AND p_to
    )
    SELECT
        p.room_id,
        p.room_code::varchar,
        p.room_name::varchar,
        p.room_type::varchar,
        SUM(p.paid_amount),
        COUNT(DISTINCT p.booking_id)::bigint,
        SUM(p.nights)::bigint,
        ROW_NUMBER() OVER (ORDER BY SUM(p.paid_amount) DESC)
    FROM paid p
    GROUP BY p.room_id, p.room_code, p.room_name, p.room_type
    ORDER BY SUM(p.paid_amount) DESC
    LIMIT p_limit
$$;

-- -----------------------------------------------------------------------------
-- 6) fn_occupancy — FUNCTION: cong suat phong theo ngay.
--
--    Hai con so khac nhau, dung nham:
--      rooms_occupied = phong co khach DA TRA TIEN ngu dem do   (goc tai chinh)
--      rooms_blocked  = phong dang bi giu (HOLD / PENDING_PAYMENT / CONFIRMED)
--                       (goc van hanh — phong nay khong ban duoc du chua thu tien)
--    Mau so la so phong is_active = true trong ky do.
-- -----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_occupancy(p_from date, p_to date)
RETURNS TABLE (
    stay_date       date,
    rooms_occupied  bigint,
    rooms_blocked   bigint,
    rooms_total     bigint,
    occupancy_rate  numeric,
    block_rate      numeric
)
LANGUAGE sql
STABLE
AS $$
    WITH days AS (
        SELECT generate_series(p_from::timestamp, p_to::timestamp, interval '1 day')::date AS d
    ),
    total AS (
        SELECT COUNT(*)::bigint AS n FROM rooms WHERE is_active = true
    ),
    occupied AS (
        SELECT n.stay_date, COUNT(DISTINCT n.room_id)::bigint AS n
        FROM v_booking_night n
        WHERE n.is_paid AND n.stay_date BETWEEN p_from AND p_to
        GROUP BY n.stay_date
    ),
    blocked AS (
        SELECT n.stay_date, COUNT(DISTINCT n.room_id)::bigint AS n
        FROM v_booking_night n
        WHERE n.booking_status IN ('HOLD', 'PENDING_PAYMENT', 'CONFIRMED', 'COMPLETED')
          AND n.stay_date BETWEEN p_from AND p_to
        GROUP BY n.stay_date
    )
    SELECT
        days.d,
        COALESCE(o.n, 0),
        COALESCE(b.n, 0),
        t.n,
        CASE WHEN t.n = 0 THEN 0 ELSE ROUND(COALESCE(o.n, 0)::numeric / t.n, 4) END,
        CASE WHEN t.n = 0 THEN 0 ELSE ROUND(COALESCE(b.n, 0)::numeric / t.n, 4) END
    FROM days
    CROSS JOIN total t
    LEFT JOIN occupied o ON o.stay_date = days.d
    LEFT JOIN blocked  b ON b.stay_date = days.d
    ORDER BY days.d
$$;

-- -----------------------------------------------------------------------------
-- 7) fn_refresh_revenue_mv — PROCEDURE: lam moi matview.
--
--    phai la PROCEDURE (goi bang CALL) chu khong phai FUNCTION vi
--    REFRESH MATERIALIZED VIEW la lenh ghi, khong chay duoc trong ham SQL thuan.
--
--    CONCURRENTLY khong khoa bang khi doc — dashboard van mo duoc trong luc
--    refresh. No doi hoi matview co unique index tren cac cot NOT NULL; neu
--    khong thoa (du lieu cu, cot suy ra nullable) thi fallback sang REFRESH
--    thuong de bao cao khong bao gio dung o ban sao cu ma khong ai biet.
-- -----------------------------------------------------------------------------
CREATE OR REPLACE PROCEDURE fn_refresh_revenue_mv()
LANGUAGE plpgsql
AS $$
BEGIN
    BEGIN
        REFRESH MATERIALIZED VIEW CONCURRENTLY mv_revenue_monthly;
    EXCEPTION WHEN OTHERS THEN
        RAISE WARNING 'REFRESH CONCURRENTLY that bai (%), chuyen sang REFRESH thuong', SQLERRM;
        REFRESH MATERIALIZED VIEW mv_revenue_monthly;
    END;
END;
$$;
