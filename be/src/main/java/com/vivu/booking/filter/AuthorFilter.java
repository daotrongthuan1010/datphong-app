package com.vivu.booking.filter;

import com.vivu.booking.dto.response.AuthTokenResponse;
import com.vivu.booking.dto.response.UsersLoginResponse;
import com.vivu.booking.service.AuthorizationService;
import com.vivu.booking.utils.JwtUtil;
import com.vivu.booking.utils.ServletUtils;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Phan quyen dua tren DB (roles -> permissions) — chay sau AuthenFilter.
 *
 * <p>Thu tu chay (Cors -> RequestId -> Authen -> Author) khai bao trong WEB-INF/web.xml,
 * khong dung @WebFilter nua vi thu tu cua @WebFilter cung url-pattern do container quyet dinh.
 *
 * <p>Nguyen tac:
 * <ul>
 *   <li>Chi enforce khi request CO danh tinh (JWT access hop le HOAC session "user").
 *       Neu khong co danh tinh → cho qua, AuthenFilter se 401 neu route can auth.</li>
 *   <li>Route khong nam trong bang phan quyen → chi can dang nhap, cho qua.</li>
 *   <li>GET public browse (rooms/health/reviews/amenities) cho ca an danh lan dang nhap → khong doi permission.</li>
 *   <li>Permission lay tu AuthorizationService (role_permissions trong DB, cache 5', lower-case key).</li>
 * </ul>
 */
public class AuthorFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        Set<String> roles = resolveRoles(req);
        if (roles == null) {
            chain.doFilter(request, response);
            return;
        }

        String path = AuthenFilter.stripContext(req);
        String required = requiredPermission(path, req.getMethod());
        if (required == null || AuthorizationService.has(roles, required)) {
            chain.doFilter(request, response);
            return;
        }
        ServletUtils.deny(req, res, HttpServletResponse.SC_FORBIDDEN,
                "Bạn không có quyền thực hiện thao tác này" + (required != null ? " (" + required + ")" : ""));
    }

    /**
     * Bang phan quyen khai bao (route → permission). Khop theo prefix + kiem tra
     * bien the /{id} so hoc de khong vo tinh chan nham prefix rong.
     *
     * <p>Khong trong route → null = chi can dang nhap (login-only), khong fail-open theo nghia
     * cap phep nham — day la lua chon thiet ke de endpoint moi chua khai bao khong tu khoa ca he thong.
     * Khi them endpoint moi, them 1 nhanh vao day.
     */
    private static String requiredPermission(String path, String method) {
        boolean read = "GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method);

        // Doc cong khai — an danh lan dang nhap deu duoc, khong doi READ.
        if (read && isPublicRead(path)) return null;
        // Luong self-service cua HostProfile — chi can dang nhap, khong can HOST_APPROVE.
        if (path.equals("/api/HostProfile/me") || path.startsWith("/api/HostProfile/me/")) return null;
        if (path.equals("/api/HostProfile") && !read) {
            // POST /api/HostProfile — host tu dang ky ho so, chi can dang nhap
            return null;
        }

        // Bao cao doanh thu — GET can REPORT_READ, POST refresh can SYSTEM_CONFIG.
        // Tach khoi /api/admin/ chung vi doanh thu la doc, config la ghi.
        if (path.startsWith("/api/admin/stats")) {
            return read ? "REPORT_READ" : "SYSTEM_CONFIG";
        }
        if (path.startsWith("/api/admin/")) return "SYSTEM_CONFIG";
        if (path.startsWith("/api/roles")) return read ? "ROLE_READ" : "ROLE_WRITE";
        if (path.startsWith("/api/users")) return read ? "USER_READ" : "USER_WRITE";
        if (path.startsWith("/api/rooms")) return read ? null : "ROOM_WRITE"; // GET cong khai o tren
        if (path.startsWith("/api/bookings")) return read ? "BOOKING_READ" : "BOOKING_WRITE";
        if (path.startsWith("/api/payments")) return read ? "PAYMENT_READ" : "PAYMENT_WRITE";
        if (path.startsWith("/api/reviews")) return read ? null : "REVIEW_WRITE"; // GET cong khai
        if (path.startsWith("/api/voucher")) return read ? "VOUCHER_READ" : "VOUCHER_WRITE";
        if (path.startsWith("/api/loyalty")) return read ? "LOYALTY_READ" : "LOYALTY_WRITE";
        if (path.startsWith("/api/amenities")) return read ? null : "AMENITY_WRITE"; // GET cong khai
        if (path.startsWith("/api/profile")) return null; // self-service — chỉ cần đăng nhập
        if (path.startsWith("/api/conversations")) return null; // chat — chỉ cần đăng nhập
        if (path.startsWith("/api/HostProfile")) {
            // Cac thao tac quan tri ho so host (duyet/tu choi/xoa/sua cua nguoi khac)
            if ("DELETE".equalsIgnoreCase(method)) return "HOST_APPROVE";
            if ("PUT".equalsIgnoreCase(method) || "PATCH".equalsIgnoreCase(method)) return "HOST_APPROVE";
            if (read) return "HOST_APPROVE"; // GET /api/HostProfile, GET /{id}
            return "HOST_APPROVE";
        }
        return null;
    }

    private static boolean isPublicRead(String path) {
        return path.startsWith("/api/rooms")
                || path.startsWith("/api/health")
                || path.startsWith("/api/reviews")
                || path.startsWith("/api/amenities");
    }

    /**
     * Tap role code cua nguoi dung, hoac null neu khong co danh tinh.
     * Nguon: JWT access (claims.roles, co the uppercase) → session "role" → session "user".
     * Da sua de doc ca AuthTokenResponse.UserSummary (AuthServlet luu) lan UsersLoginResponse (LoginServlet cu).
     */
    @SuppressWarnings("unchecked")
    private static Set<String> resolveRoles(HttpServletRequest req) {
        String authHeader = req.getHeader("Authorization");
        if (authHeader != null && authHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
            try {
                JwtUtil.Claims claims = JwtUtil.parse(authHeader.substring(7).trim());
                if ("access".equals(claims.getType())) {
                    Set<String> roles = claims.getRoles();
                    return roles == null ? Set.of() : roles;
                }
            } catch (Exception ignored) {
            }
        }
        HttpSession session = req.getSession(false);
        if (session != null) {
            Object roleAttr = session.getAttribute("role");
            if (roleAttr instanceof Set<?> set) {
                Set<String> out = new HashSet<>();
                for (Object o : set) out.add(String.valueOf(o));
                return out;
            }
            if (roleAttr instanceof String s) {
                return Set.of(s);
            }
            Object userAttr = session.getAttribute("user");
            if (userAttr instanceof AuthTokenResponse.UserSummary us) {
                Set<String> r = us.getRoles();
                return r != null ? new HashSet<>(r) : Set.of();
            }
            if (userAttr instanceof UsersLoginResponse ul) {
                Set<String> r = ul.getRole();
                return r != null ? new HashSet<>(r) : Set.of();
            }
            if (userAttr != null) {
                return Set.of();
            }
        }
        return null;
    }
}
