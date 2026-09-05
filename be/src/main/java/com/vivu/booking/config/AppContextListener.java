package com.vivu.booking.config;

import com.vivu.booking.entity.Role;
import com.vivu.booking.entity.User;
import com.vivu.booking.enums.UserStatus;
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
 * Khởi động: seed roles (admin, host, user) và admin demo: admin / 123456.
 * Schema (cột 2FA, kiểu cột status, ...) do Hibernate hbm2ddl.auto=update quản lý,
 * không chạy ALTER TABLE thủ công ở đây nữa.
 */
@WebListener
public class AppContextListener implements ServletContextListener {
    private static final Logger log = LoggerFactory.getLogger(AppContextListener.class);

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        log.info("=== VIVU Booking BE starting ===");
        try {
            var sf = HibernateConfig.getSessionFactory();
            log.info("SessionFactory initialized: {}", sf != null);
            seedData();
        } catch (Throwable e) {
            log.error("DB not reachable at startup - app will start, DB ops will fail until DB is reachable: {}", e.toString());
        }
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
                tx.commit();
            } catch (Exception e) {
                tx.rollback();
                log.error("Seed failed: {}", e.toString());
            }
        } catch (Exception e) {
            log.error("Seed init failed: {}", e.toString());
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        log.info("Shutting down VIVU Booking BE");
        try {
            HibernateConfig.shutdown();
        } catch (Throwable ignored) {
        }
        try {
            RedisConfig.shutdown();
        } catch (Throwable ignored) {
        }
    }
}
