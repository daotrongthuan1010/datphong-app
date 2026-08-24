package com.vivu.booking.config;

import com.vivu.booking.entity.Room;
import com.vivu.booking.utils.AppProperties;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public final class HibernateConfig {

    private static final Logger log = LoggerFactory.getLogger(HibernateConfig.class);
    private static SessionFactory sessionFactory;

    private HibernateConfig() {
    }

    public static synchronized SessionFactory getSessionFactory() {
        if (sessionFactory == null) {
            try {
                Properties props = new Properties();
                props.put("hibernate.connection.driver_class", "org.postgresql.Driver");
                props.put("hibernate.connection.url", AppProperties.get("app.datasource.vivu.url"));
                props.put("hibernate.connection.username", AppProperties.get("app.datasource.vivu.username"));
                props.put("hibernate.connection.password", AppProperties.get("app.datasource.vivu.password"));
                props.put("hibernate.dialect", AppProperties.get("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect"));
                props.put("hibernate.hbm2ddl.auto", AppProperties.get("hibernate.hbm2ddl.auto", "update"));
                props.put("hibernate.show_sql", AppProperties.get("hibernate.show_sql", "false"));
                props.put("hibernate.format_sql", AppProperties.get("hibernate.format_sql", "true"));
                props.put("hibernate.jdbc.batch_size", AppProperties.get("hibernate.jdbc.batch_size", "20"));
                // HikariCP via Hibernate provider
                props.put("hibernate.connection.provider_class", "org.hibernate.hikaricp.internal.HikariCPConnectionProvider");
                props.put("hibernate.hikari.maximumPoolSize", String.valueOf(AppProperties.getInt("app.datasource.vivu.pool.size", 10)));
                props.put("hibernate.hikari.minimumIdle", String.valueOf(AppProperties.getInt("app.datasource.vivu.pool.minimumIdle", 2)));
                props.put("hibernate.hikari.poolName", "vivu-hikari");
                props.put("hibernate.hikari.connectionTimeout", "30000");
                props.put("hibernate.hikari.idleTimeout", "600000");

                Configuration cfg = new Configuration();
                cfg.setProperties(props);
                cfg.addAnnotatedClass(Room.class);

                ServiceRegistry registry = new StandardServiceRegistryBuilder()
                        .applySettings(cfg.getProperties()).build();
                sessionFactory = cfg.buildSessionFactory(registry);
                log.info("Hibernate SessionFactory initialized url={}", props.get("hibernate.connection.url"));
            } catch (Exception e) {
                log.error("Failed to init SessionFactory", e);
                throw new ExceptionInInitializerError(e);
            }
        }
        return sessionFactory;
    }

    public static synchronized void shutdown() {
        if (sessionFactory != null) {
            sessionFactory.close();
            sessionFactory = null;
        }
        log.info("Hibernate shutdown complete");
    }
}
