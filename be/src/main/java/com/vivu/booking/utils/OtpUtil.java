package com.vivu.booking.utils;

import java.security.SecureRandom;

public final class OtpUtil {
    private static final SecureRandom RANDOM = new SecureRandom();

    private OtpUtil() {
    }

    public static String generate6Digit() {
        int n = RANDOM.nextInt(1_000_000);
        return String.format("%06d", n);
    }
}
