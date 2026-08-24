package com.vivu.booking.config;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebListener
public class AppContextListener implements ServletContextListener {
    private static final Logger log = LoggerFactory.getLogger(AppContextListener.class);

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        log.info("=== VIVU Booking BE starting ===");
        try {
            var sf = HibernateConfig.getSessionFactory();
            log.info("SessionFactory initialized: {}", sf != null);
        } catch (Throwable e) {
            log.error("DB not reachable at startup - app will start, DB ops will fail until DB is reachable: {}", e.toString());
        }
        log.info("=== VIVU Booking BE started ===");
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
