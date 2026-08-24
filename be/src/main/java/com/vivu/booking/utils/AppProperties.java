package com.vivu.booking.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AppProperties {

    private static final Properties PROPS = new Properties();

    static {
        try (InputStream in = AppProperties.class.getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (in != null) PROPS.load(in);
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
        // Allow env / system props to override file
        PROPS.putAll(System.getProperties());
        System.getenv().forEach((k, v) -> {
            String dotted = k.toLowerCase().replace('_', '.');
            PROPS.putIfAbsent(dotted, v);
            PROPS.putIfAbsent(k, v);
        });
    }

    public static String get(String key) {
        return PROPS.getProperty(key);
    }

    public static String get(String key, String def) {
        return PROPS.getProperty(key, def);
    }

    public static int getInt(String key, int def) {
        String v = PROPS.getProperty(key);
        if (v == null) return def;
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public static long getLong(String key, long def) {
        String v = PROPS.getProperty(key);
        if (v == null) return def;
        try {
            return Long.parseLong(v.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public static boolean getBool(String key, boolean def) {
        String v = PROPS.getProperty(key);
        return v == null ? def : Boolean.parseBoolean(v.trim());
    }
}
