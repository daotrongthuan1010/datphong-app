package com.vivu.booking.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vivu.booking.exception.BusinessException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

/**
 * JWT (HS256) tự viết tay - KHÔNG cần thêm thư viện jjwt vào pom.xml, tận
 * dụng jackson-databind (đã có sẵn) để encode/decode payload JSON.
 * Cấu hình đọc từ application.properties (có default nếu thiếu):
 *   jwt.secret=...
 *   jwt.access-token-minutes=30
 *   jwt.refresh-token-days=7
 */
public final class JwtUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String HEADER_JSON = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";

    private static final String SECRET = AppProperties.get(
            "jwt.secret", "CHANGE_ME_vivu_booking_dev_secret_key_2026_please_override");
    private static final long ACCESS_MINUTES = AppProperties.getLong("jwt.access-token-minutes", 30);
    private static final long REFRESH_DAYS = AppProperties.getLong("jwt.refresh-token-days", 7);

    private JwtUtil() {
    }

    public static String generateAccessToken(Long userId, String username, Set<String> roles) {
        return build(userId, username, roles, "access", ACCESS_MINUTES * 60);
    }

    public static String generateRefreshToken(Long userId) {
        return build(userId, null, null, "refresh", REFRESH_DAYS * 24 * 3600);
    }

    public static long accessTokenTtlSeconds() {
        return ACCESS_MINUTES * 60;
    }

    public static long refreshTokenTtlSeconds() {
        return REFRESH_DAYS * 24 * 3600;
    }

    private static String build(Long userId, String username, Set<String> roles, String type, long ttlSeconds) {
        long now = Instant.now().getEpochSecond();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", String.valueOf(userId));
        if (username != null) payload.put("username", username);
        if (roles != null) payload.put("roles", roles);
        payload.put("type", type);
        payload.put("iat", now);
        payload.put("exp", now + ttlSeconds);

        String headerB64 = base64Url(HEADER_JSON.getBytes(StandardCharsets.UTF_8));
        String payloadB64 = base64Url(writeJson(payload).getBytes(StandardCharsets.UTF_8));
        String signingInput = headerB64 + "." + payloadB64;
        return signingInput + "." + sign(signingInput);
    }

    public static Claims parse(String token) {
        return parse(token, true);
    }

    /** verifyExpiry=false dùng cho logout: vẫn verify chữ ký, chỉ bỏ qua kiểm tra hết hạn. */
    //kiểm tra code
    public static Claims parse(String token, boolean verifyExpiry) {
        if (token == null || token.isBlank()) {
            throw new BusinessException(401, "Token không được để trống");
        }
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new BusinessException(401, "Token không hợp lệ");
        }
        String signingInput = parts[0] + "." + parts[1];
        if (!sign(signingInput).equals(parts[2])) {
            throw new BusinessException(401, "Token không hợp lệ (sai chữ ký)");
        }

        Map<?, ?> payload;
        try {
            payload = MAPPER.readValue(base64UrlDecode(parts[1]), Map.class);
        } catch (Exception e) {
            throw new BusinessException(401, "Token không hợp lệ (payload lỗi)");
        }

        long exp = ((Number) payload.get("exp")).longValue();
        if (verifyExpiry && Instant.now().getEpochSecond() > exp) {
            throw new BusinessException(401, "Token đã hết hạn, vui lòng đăng nhập lại");
        }
        return new Claims(payload);
    }

    private static String sign(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return base64Url(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException("Không thể ký JWT", e);
        }
    }

    private static String base64Url(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    private static byte[] base64UrlDecode(String s) {
        return Base64.getUrlDecoder().decode(s);
    }

    private static String writeJson(Object o) {
        try {
            return MAPPER.writeValueAsString(o);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static final class Claims {
        private final Map<?, ?> data;

        private Claims(Map<?, ?> data) {
            this.data = data;
        }

        public Long getUserId() {
            return Long.valueOf(String.valueOf(data.get("sub")));
        }

        public String getUsername() {
            return (String) data.get("username");
        }

        @SuppressWarnings("unchecked")
        public Set<String> getRoles() {
            Object r = data.get("roles");
            return r == null ? Set.of() : new HashSet<>((Collection<String>) r);
        }

        public String getType() {
            return (String) data.get("type");
        }
    }
}
