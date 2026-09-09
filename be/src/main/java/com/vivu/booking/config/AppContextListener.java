package com.vivu.booking.config;

import com.vivu.booking.entity.LoyaltyRank;
import com.vivu.booking.entity.Permission;
import com.vivu.booking.entity.Role;
import com.vivu.booking.entity.User;
import com.vivu.booking.enums.RankNameType;
import com.vivu.booking.enums.UserStatus;
import com.vivu.booking.scheduler.HoldExpireScheduler;
import com.vivu.booking.service.AuthorizationService;
import com.vivu.booking.service.PermissionCatalog;
import com.vivu.booking.utils.PasswordUntil;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * Listener khởi động ứng dụng:
 * <ul>
 *   <li>Khởi tạo Hibernate SessionFactory (quét entity).</li>
 *   <li>Seed roles + user admin mặc định (idempotent — chạy lại không tạo trùng).</li>
 *   <li>Khởi động HoldExpireScheduler (mỗi 5 phút quét HOLD hết hạn → EXPIRED + trả lịch).</li>
 *   <li>contextDestroyed: dừng scheduler, đóng SessionFactory & Redis pool.</li>
 * </ul>
 */
@WebListener
public class AppContextListener implements ServletContextListener {
    private static final Logger log = LoggerFactory.getLogger(AppContextListener.class);
    private HoldExpireScheduler holdExpireScheduler;
    /** Matview mua vu tinh profit + VAT, co priceOverride — preserve 2024 business logic. */
    private com.vivu.booking.scheduler.RevenueRefreshScheduler revenueRefreshScheduler;

    /**
     * Cờ báo lần seed này có thực sự ghi thêm permissions/role_permissions hay không.
     * Chỉ dùng để quyết định có gọi {@link AuthorizationService#invalidate()} — tránh
     * xoá cache phân quyền ở mỗi lần khởi động khi DB đã đủ dữ liệu.
     */
    private boolean permissionsChanged;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        log.info("=== VIVU Booking BE starting ===");
        try {
            var sf = HibernateConfig.getSessionFactory();
            log.info("SessionFactory initialized: {}", sf != null);
            // Chay schema patch idempotent TRUOC khi seed/query nghiep vu: bo sung cac cot
            // NOT NULL ma hbm2ddl.auto=update khong the them vao bang da co du lieu
            // (vd payments.currency — nguyen nhan "Internal server error" khi bam Thanh toan).
            if (com.vivu.booking.utils.AppProperties.getBool("db.schema-patch.auto-run", true)) {
                int patched = com.vivu.booking.utils.SqlScriptRunner.runClasspathScript("/db/schema-patch.sql");
                log.info("Schema patch chay xong ({} statements)", patched);
            }
            seedData();
        } catch (Throwable e) {
            log.error("DB not reachable at startup — app starts, DB ops fail until DB reachable: {}", e.toString());
        }
        holdExpireScheduler = new HoldExpireScheduler();
        holdExpireScheduler.start();
        if (com.vivu.booking.utils.AppProperties.getBool("db.report-objects.auto-create", true)) {
            int ran = com.vivu.booking.utils.SqlScriptRunner.runClasspathScript("/db/revenue-objects.sql");
            log.info("Revenue DB objects tao/khai bao xong (chay {}/{} statements)",
                    ran, "/db/revenue-objects.sql");
        }
        revenueRefreshScheduler = new com.vivu.booking.scheduler.RevenueRefreshScheduler();
        revenueRefreshScheduler.start();
        log.info("=== VIVU Booking BE started ===");
    }

    private void seedData() {
        try (Session session = HibernateConfig.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();
            try {
                String[] codes = {"admin", "host", "user"};
                String[] names = {"Quan tri vien", "Chu nha", "Khach hang"};
                Set<Role> adminRoles = new HashSet<>();
                for (int i = 0; i < codes.length; i++) {
                    Role r = session.createQuery("from Role where code = :c", Role.class)
                            .setParameter("c", codes[i]).uniqueResult();
                    if (r == null) {
                        r = new Role();
                        r.setCode(codes[i]);
                        r.setName(names[i]);
                        r.setDescription(names[i] + " (auto seed)");
                        session.persist(r);
                        log.info("Seeded role {}", codes[i]);
                    }
                    if ("admin".equals(codes[i])) adminRoles.add(r);
                    if ("user".equals(codes[i])) adminRoles.add(r);
                }
                User admin = session.createQuery("from User where username = :u", User.class)
                        .setParameter("u", "admin").uniqueResult();
                if (admin == null) {
                    admin = User.builder()
                            .fullName("Quan tri vien")
                            .email("admin@vivu.local")
                            .phone("0900000001")
                            .username("admin")
                            .password(PasswordUntil.hashedPassword("123456"))
                            .gender(true)
                            .status(UserStatus.ACTIVE)
                            .active(true)
                            .role(adminRoles)
                            .build();
                    session.persist(admin);
                    log.info("Seeded admin user (username=admin password=123456)");
                }
                seedPermissions(session);
                tx.commit();
                if (permissionsChanged) {
                    // role_permissions vua doi -> bang quyen trong cache cua AuthorFilter da cu
                    AuthorizationService.invalidate();
                }
            } catch (Exception e) {
                tx.rollback();
                log.error("Seed failed: {}", e.toString());
            }
        } catch (Exception e) {
            log.error("Seed init failed: {}", e.toString());
        }
        try (Session loyaltySession = HibernateConfig.getSessionFactory().openSession()) {
            seedLoyaltyRanks(loyaltySession);
        } catch (Exception e) {
            log.warn("Seed loyalty_ranks that bai (bo qua, van chay duoc): {}", e.toString());
        }
    }

    /**
     * Seed {@code permissions} + {@code role_permissions} — idempotent, gan quyen theo
     * <b>role.code</b> (khong theo id).
     *
     * <p>Ly do: bang do duoc Hibernate tu tao ({@code hbm2ddl.auto=update}) va <b>rong</b>
     * tren DB moi. {@link com.vivu.booking.filter.AuthorFilter} lay quyen tu DB, nen DB rong
     * = moi route co permission deu 403 (ke ca admin). {@code documents/seed-data.sql} gan
     * quyen theo {@code role_id} hardcode nen khong dung khi id that khac.
     *
     * <p>Danh muc quyen + phan bo mac dinh: {@link PermissionCatalog}.
     */
    private void seedPermissions(Session session) {
        for (var e : PermissionCatalog.ALL.entrySet()) {
            String code = e.getKey();
            Permission p = session.createQuery("from Permission where code = :c", Permission.class)
                    .setParameter("c", code).uniqueResult();
            if (p == null) {
                p = Permission.builder()
                        .code(code)
                        .name(e.getValue()[0])
                        .description(e.getValue()[1])
                        .build();
                session.persist(p);
                permissionsChanged = true;
            }
        }
        // Bat buoc flush: query native ben duoi doc bang permissions ma Hibernate khong
        // biet de tu flush (khong xac dinh duoc "query space" cua SQL tho).
        session.flush();

        int linked = 0;
        for (var e : PermissionCatalog.BY_ROLE.entrySet()) {
            for (String permCode : e.getValue()) {
                linked += session.createNativeQuery("""
                        INSERT INTO role_permissions(role_id, permission_id)
                        SELECT r.id, p.id FROM roles r, permissions p
                        WHERE lower(r.code) = :roleCode AND p.code = :permCode
                          AND NOT EXISTS (
                              SELECT 1 FROM role_permissions rp
                              WHERE rp.role_id = r.id AND rp.permission_id = p.id)
                        """)
                        .setParameter("roleCode", e.getKey())
                        .setParameter("permCode", permCode)
                        .executeUpdate();
            }
        }
        if (linked > 0) {
            permissionsChanged = true;
            log.info("Seeded {} role_permissions lien ket moi", linked);
        }
    }

    /** 5 hạng mặc định cho loyalty — idempotent, chỉ tạo khi chưa có. */
    private void seedLoyaltyRanks(Session session) {
        Transaction tx = session.beginTransaction();
        try {
            Long existing = session.createQuery("select count(r) from LoyaltyRank r", Long.class).getSingleResult();
            if (existing != null && existing > 0) {
                tx.commit();
                return;
            }
            RankNameType[] names = {RankNameType.MEMBER, RankNameType.SILVER, RankNameType.GOLD, RankNameType.PLATINUM, RankNameType.DIAMOND};
            int[] mins = {0, 1000, 5000, 15000, 30000};
            String[] discounts = {"0", "3", "5", "8", "12"};
            for (int i = 0; i < names.length; i++) {
                LoyaltyRank r = LoyaltyRank.builder()
                        .name(names[i])
                        .minPoints(mins[i])
                        .discountPercent(new java.math.BigDecimal(discounts[i]))
                        .build();
                session.persist(r);
            }
            tx.commit();
            log.info("Seeded 5 loyalty_ranks");
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            log.warn("Seed loyalty_ranks that bai: {}", e.toString());
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        log.info("Shutting down VIVU Booking BE");
        if (revenueRefreshScheduler != null) {
            try { revenueRefreshScheduler.stop(); } catch (Throwable ignored) {}
        }
        if (holdExpireScheduler != null) {
            try { holdExpireScheduler.stop(); } catch (Throwable ignored) {}
        }
        try { HibernateConfig.shutdown(); } catch (Throwable ignored) {}
        try { RedisConfig.shutdown(); } catch (Throwable ignored) {}
    }
}
