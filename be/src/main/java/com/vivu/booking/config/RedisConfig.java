package com.vivu.booking.config;

import com.vivu.booking.utils.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public final class RedisConfig {
    private static final Logger log = LoggerFactory.getLogger(RedisConfig.class);
    private static JedisPool pool;

    private RedisConfig() {
    }

    public static synchronized JedisPool getPool() {
        if (pool == null) {
            String host = AppProperties.get("spring.data.redis.host", "103.216.117.40");
            int port = AppProperties.getInt("spring.data.redis.port", 6379);
            String password = AppProperties.get("spring.data.redis.password", "hoclaptrinh@2026");
            int db = AppProperties.getInt("spring.data.redis.database", 1);
            int timeout = AppProperties.getInt("redis.timeout.millis", 2000);
            JedisPoolConfig cfg = new JedisPoolConfig();
            cfg.setMaxTotal(20);
            cfg.setMaxIdle(10);
            cfg.setMinIdle(2);
            if (password != null && !password.isBlank()) {
                pool = new JedisPool(cfg, host, port, timeout, password, db);
            } else {
                pool = new JedisPool(cfg, host, port, timeout, null, db);
            }
            log.info("Redis pool initialized {}:{} (db={})", host, port, db);
        }
        return pool;
    }

    public static synchronized void shutdown() {
        if (pool != null) {
            pool.close();
            pool = null;
            log.info("Redis pool closed");
        }
    }
}
