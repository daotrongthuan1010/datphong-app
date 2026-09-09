package com.vivu.booking.utils;

import com.vivu.booking.config.HibernateConfig;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Chay mot file SQL (classpath resource) thanh tung statement.
 *
 * <h2>Vì sao tồn tại lớp này</h2>
 * <p>
 * {@code hibernate.hbm2ddl.auto=update} chỉ tạo được <b>bảng</b> từ entity. Nó không biết gì về
 * VIEW, MATERIALIZED VIEW hay FUNCTION/PROCEDURE — những thứ dashboard doanh thu cần để đẩy
 * aggregate xuống DB thay vì viết đi viết lại trong Java.
 *
 * <h2>Vì sao không {@code split(";")}</h2>
 * <p>
 * Thân function nằm giữa cặp {@code $$ ... $$} và bên trong có rất nhiều dấu {@code ;}.
 * Cắt thô theo {@code ;} sẽ xé một function thành nhiều mảnh vô nghĩa. Bộ tách ở đây đi
 * quét từng ký tự và <b>bỏ qua</b> dấu {@code ;} nằm trong: chuỗi {@code '...'}, định danh
 * {@code "..."}, comment {@code --} / {@code /* *}{@code /}, và dollar-quote {@code $$...$$}
 * hoặc {@code $tag$...$tag$}.
 *
 * <h2>Vì sao lỗi không được làm sập app</h2>
 * <p>
 * DB user có thể thiếu quyền {@code CREATE VIEW}, hoặc lần start đầu bảng chưa tồn tại.
 * Đổi lấy dashboard mà BE không khởi động được là đánh đổi sai. Vì vậy lỗi chỉ log, còn
 * tầng Service trả 503 kèm thông báo "chưa tạo được DB objects" — thẳng thắn báo chưa có
 * số liệu, thay vì im lặng trả về 0 (số 0 giả còn nguy hiểm hơn một lỗi nhìn thấy được).
 */
public final class SqlScriptRunner {

    private static final Logger log = LoggerFactory.getLogger(SqlScriptRunner.class);

    private SqlScriptRunner() {
    }

    /**
     * Đọc resource từ classpath và chạy script.
     *
     * @param  classpathResource đường dẫn bắt đầu bằng {@code /}, vd {@code /db/revenue-objects.sql}
     * @return số statement trong script khi chạy hết, {@code 0} khi đã rollback do lỗi
     */
    public static int runClasspathScript(String classpathResource) {
        try (InputStream in = SqlScriptRunner.class.getResourceAsStream(classpathResource)) {
            if (in == null) {
                log.warn("Khong tim thay SQL resource {} — bo qua", classpathResource);
                return 0;
            }
            String sql = readAll(in);
            return runStatements(splitStatements(sql));
        } catch (Exception e) {
            log.error("Khong doc duoc {}: {}", classpathResource, e.toString());
            return 0;
        }
    }

    /**
     * Chạy toàn bộ statement trong MỘT transaction, qua JDBC thô.
     *
     * <h2>Vì sao không dùng {@code session.createNativeQuery(...)}?</h2>
     * <p>
     * Hibernate coi {@code :} là dấu mở đầu <b>tham số đặt tên</b>. SQL của PostgreSQL lại dùng
     * {@code ::} để cast kiểu ({@code x::date}, {@code status::text}). Đi qua parser của ORM thì
     * {@code ::date} bị ăn mất một dấu {@code :} và câu lệnh xuống DB thành {@code ):date} —
     * lỗi {@code syntax error at or near ":"} mà nhìn log rất khó hiểu vì file SQL hoàn toàn đúng.
     * DDL không có tham số nào cần bind, nên đi thẳng JDBC là vừa đúng vừa đơn giản hơn.
     *
     * <h2>Vì sao một transaction cho cả script?</h2>
     * <p>
     * DDL của PostgreSQL có transactional. Các object này phụ thuộc nhau theo dây chuyền
     * ({@code mv_revenue_monthly} đọc {@code v_booking_night}, {@code fn_revenue_summary} đọc
     * {@code v_revenue_daily}). Chạy nửa vời để lại trạng thái không nhất quán và rất khó lần.
     * Một câu lỗi thì rollback hết: hoặc có đủ bộ báo cáo, hoặc không có gì và Service trả 503
     * kèm thông báo rõ — tốt hơn là dashboard âm thầm hiện số 0.
     *
     * <p>Lưu ý: sau khi một câu lỗi, transaction của PG ở trạng thái "aborted" nên mọi câu tiếp
     * theo đều fail — vì vậy phải dừng ngay chứ không thể bỏ qua rồi chạy tiếp.
     */
    private static int runStatements(List<String> statements) {
        if (statements.isEmpty()) return 0;
        try (Session s = HibernateConfig.getSessionFactory().openSession()) {
            var tx = s.beginTransaction();
            try {
                s.doWork(conn -> {
                    try (Statement st = conn.createStatement()) {
                        for (String stmt : statements) {
                            try {
                                st.execute(stmt);
                            } catch (SQLException e) {
                                throw new SQLException(firstLine(stmt) + " — " + e.getMessage(), e);
                            }
                        }
                    }
                });
                tx.commit();
                return statements.size();
            } catch (Exception e) {
                if (tx.isActive()) tx.rollback();
                log.error("Tao doi tuong bao cao that bai (da rollback toan bo): {}", rootMessage(e));
                return 0;
            }
        }
    }

    private static String rootMessage(Throwable e) {
        Throwable cur = e;
        String msg = cur.getMessage();
        while (cur.getCause() != null && cur.getCause() != cur) {
            cur = cur.getCause();
            if (cur.getMessage() != null && !cur.getMessage().isBlank()) msg = cur.getMessage();
        }
        return msg == null ? e.toString() : msg;
    }

    private static String firstLine(String stmt) {
        String t = stmt.trim();
        int nl = t.indexOf('\n');
        return (nl > 0 ? t.substring(0, nl) : t).replaceAll("\\s+", " ").trim();
    }

    private static String readAll(InputStream in) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) sb.append(line).append('\n');
        }
        return sb.toString();
    }

    /**
     * Tách SQL thành các statement theo dấu {@code ;} ở ngoài chuỗi/comment/dollar-quote.
     */
    static List<String> splitStatements(String sql) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        int i = 0;
        int n = sql.length();
        while (i < n) {
            char c = sql.charAt(i);

            // Comment dong -- den het dong
            if (c == '-' && i + 1 < n && sql.charAt(i + 1) == '-') {
                while (i < n && sql.charAt(i) != '\n') i++;
                continue;
            }
            // Comment khoi /* ... */
            if (c == '/' && i + 1 < n && sql.charAt(i + 1) == '*') {
                int end = sql.indexOf("*/", i + 2);
                i = end < 0 ? n : end + 2;
                continue;
            }
            // Chuoi '...' (ho tro '' escape)
            if (c == '\'') {
                cur.append(c);
                i++;
                while (i < n) {
                    char q = sql.charAt(i);
                    cur.append(q);
                    i++;
                    if (q == '\'' && i < n && sql.charAt(i) == '\'') {
                        cur.append(sql.charAt(i));
                        i++;
                        continue;
                    }
                    if (q == '\'') break;
                }
                continue;
            }
            // Dinh danh "..."
            if (c == '"') {
                cur.append(c);
                i++;
                while (i < n) {
                    char q = sql.charAt(i);
                    cur.append(q);
                    i++;
                    if (q == '"') break;
                }
                continue;
            }
            // Dollar-quote $$ ... $$ hoac $tag$ ... $tag$
            if (c == '$') {
                String tag = readDollarTag(sql, i);
                if (tag != null) {
                    cur.append(tag);
                    int end = sql.indexOf(tag, i + tag.length());
                    if (end < 0) { // dollar-quote khong dong — lay het phan con lai
                        cur.append(sql.substring(i + tag.length()));
                        i = n;
                    } else {
                        cur.append(sql, i + tag.length(), end + tag.length());
                        i = end + tag.length();
                    }
                    continue;
                }
            }
            if (c == ';') {
                String stmt = cur.toString().trim();
                if (!stmt.isEmpty()) out.add(stmt);
                cur.setLength(0);
                i++;
                continue;
            }
            cur.append(c);
            i++;
        }
        String tail = cur.toString().trim();
        if (!tail.isEmpty()) out.add(tail);
        return out;
    }

    /** Tra ve tag dollar-quote ($$ hoac $ten$) tai vi tri i, hoac null neu khong phai. */
    private static String readDollarTag(String sql, int i) {
        int j = i + 1;
        while (j < sql.length() && (Character.isLetterOrDigit(sql.charAt(j)) || sql.charAt(j) == '_')) j++;
        if (j < sql.length() && sql.charAt(j) == '$') return sql.substring(i, j + 1);
        return null;
    }
}
