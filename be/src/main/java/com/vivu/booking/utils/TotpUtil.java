package com.vivu.booking.utils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;

/**
 * TOTP (RFC 6238) tu viet tay - tuong thich Google Authenticator, Microsoft
 * Authenticator, Authy... (tat cả đều theo chuan HOTP/TOTP).
 *
 * - Secret: 20 ngau nhien (160 bit), ma hoa Base32 (RFC 4648, khong padding) de
 *   app authenticator doc duoc.
 * - Code:   6 so, HMAC-SHA1(secret, floor(unixTime/30)), dynamic truncation.
 * - Window: mac dinh +/-1 buoc 30s de chenh lech dong ho giua may va app.
 *
 * Cau hinh (application.properties, co default neu thieu):
 *   twofactor.issuer=VIVU Booking
 *   twofactor.window-steps=1
 *   twofactor_digits=6
 *   twofactor.period-seconds=30
 */
public final class TotpUtil {

    private static final String BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final SecureRandom RANDOM = new SecureRandom();

    private static final int DIGITS = AppProperties.getInt("twofactor.digits", 6);
    private static final int PERIOD_SECONDS = AppProperties.getInt("twofactor.period-seconds", 30);
    private static final int WINDOW_STEPS = AppProperties.getInt("twofactor.window-steps", 1);
    private static final String ISSUER = AppProperties.get("twofactor.issuer", "VIVU Booking");

    private TotpUtil() {
    }

    /** Sinh secretmoi, tra ve dang Base32 (luu vao DB / hien cho user scan QR). */
    public static String generateSecret() {
        byte[] buf = new byte[20]; // 160 bit, chuan cho khoa HMAC-SHA1
        RANDOM.nextBytes(buf);
        return base32Encode(buf);
    }

    public static int generateCode(String base32Secret) {
        return generateCode(base32Secret, Instant.now().getEpochSecond());
    }

    /** Tinh ma TOTP tai mot moc thoi gian cu the (giup test / kiem tra window). */
    public static int generateCode(String base32Secret, long epochSeconds) {
        long counter = Math.floorDiv(epochSeconds, PERIOD_SECONDS);
        byte[] key = base32Decode(base32Secret);
        byte[] hash = hmacSha1(key, counter);

        // Dynamic truncation (RFC 4226 section 5.4)
        int offset = hash[hash.length - 1] & 0x0F;
        int binary = ((hash[offset] & 0x7F) << 24)
                | ((hash[offset + 1] & 0xFF) << 16)
                | ((hash[offset + 2] & 0xFF) << 8)
                | (hash[offset + 3] & 0xFF);
        int modulus = (int) Math.pow(10, DIGITS);
        return binary % modulus;
    }

    /**
     * Xac thuc ma 6 so user nhap, cho phep lech +/- WINDOW_STEPS buoc thoi gian.
     * So sanh hang so thoi gian (constant-time) de tranh timing attack.
     */
    public static boolean verify(String base32Secret, String codeInput) {
        if (base32Secret == null || base32Secret.isBlank() || codeInput == null) {
            return false;
        }
        int candidate;
        try {
            candidate = Integer.parseInt(codeInput.trim());
        } catch (NumberFormatException e) {
            return false;
        }
        long now = Instant.now().getEpochSecond();
        for (int w = -WINDOW_STEPS; w <= WINDOW_STEPS; w++) {
            int expected = generateCode(base32Secret, now + (long) w * PERIOD_SECONDS);
            if (constantTimeEquals(expected, candidate)) {
                return true;
            }
        }
        return false;
    }

    public static String getIssuer() {
        return ISSUER;
    }

    /**
     * Dung duong dan otpauth:// de app (Google/Microsoft Authenticator) sinh QR.
     * Frontend co the render QR tu chuoi nay.
     */
    public static String buildOtpAuthUri(String accountName, String base32Secret) {
        String label = urlEncode(ISSUER) + ":" + urlEncode(accountName);
        return "otpauth://totp/" + label
                + "?secret=" + base32Secret
                + "&issuer=" + urlEncode(ISSUER)
                + "&algorithm=SHA1&digits=" + DIGITS + "&period=" + PERIOD_SECONDS;
    }

    // ---------------------------------------------------------------- Base32

    static String base32Encode(byte[] data) {
        StringBuilder sb = new StringBuilder();
        int buffer = 0;
        int bitsLeft = 0;
        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xFF);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                int index = (buffer >> (bitsLeft - 5)) & 0x1F;
                bitsLeft -= 5;
                sb.append(BASE32_ALPHABET.charAt(index));
            }
        }
        if (bitsLeft > 0) {
            int index = (buffer << (5 - bitsLeft)) & 0x1F;
            sb.append(BASE32_ALPHABET.charAt(index));
        }
        return sb.toString();
    }

    static byte[] base32Decode(String encoded) {
        String data = encoded.trim().replace("=", "").replace(" ", "").toUpperCase();
        int buffer = 0;
        int bitsLeft = 0;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (char c : data.toCharArray()) {
            int val = BASE32_ALPHABET.indexOf(c);
            if (val < 0) {
                continue; // bo qua ky tu khong hop le (dau cach, gach ngang...)
            }
            buffer = (buffer << 5) | val;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                out.write((buffer >> (bitsLeft - 8)) & 0xFF);
                bitsLeft -= 8;
            }
        }
        return out.toByteArray();
    }

    // ---------------------------------------------------------------- helpers

    private static byte[] hmacSha1(byte[] key, long counter) {
        try {
            byte[] data = new byte[8];
            for (int i = 7; i >= 0; i--) {
                data[i] = (byte) (counter & 0xFF);
                counter >>= 8;
            }
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            return mac.doFinal(data);
        } catch (Exception e) {
            throw new RuntimeException("Khong the tinh HMAC cho TOTP", e);
        }
    }

    private static boolean constantTimeEquals(int a, int b) {
        return (a ^ b) == 0;
    }

    private static String urlEncode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
