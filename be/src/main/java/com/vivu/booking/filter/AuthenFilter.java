package com.vivu.booking.filter;

import com.vivu.booking.utils.JwtUtil;
import com.vivu.booking.utils.ServletUtils;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Xac thuc (authentication) — tra 401 neu request can dang nhap ma khong co danh tinh.
 *
 * <p>Thu tu chay (Cors → RequestId → Authen → Author) khai bao trong WEB-INF/web.xml,
 * khong dung @WebFilter nua vi thu tu cua @WebFilter cung url-pattern do container quyet dinh.
 *
 * <p>Nhung route public: /login (form cu), /api/auth/* (tru /logout), GET danh muc/chi tiet
 * phong - review - tien nghi - health, webhook + return cua cong thanh toan, tai lieu API.
 */
public class AuthenFilter implements Filter {

    /** Danh muc doc duoc khong can dang nhap — trung voi route public trong AuthorFilter. */
    private static final String[] PUBLIC_READ_PREFIXES = {
            "/api/rooms", "/api/health", "/api/reviews", "/api/amenities"
    };

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        // Preflight CORS khong mang cookie/Authorization — CorsFilter da dung 204 trc,
        // keep guard o day cho chac chan neu thu tu filter bi doi.
        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        String path = stripContext(req);
        if (isPublic(req, path)) {
            chain.doFilter(request, response);
            return;
        }

        // 1) JWT Bearer — FE dung access token, khong phu thuoc HttpSession.
        String authHeader = req.getHeader("Authorization");
        if (authHeader != null && authHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
            try {
                JwtUtil.Claims claims = JwtUtil.parse(authHeader.substring(7).trim());
                if ("access".equals(claims.getType())) {
                    chain.doFilter(request, response);
                    return;
                }
            } catch (Exception ignored) {
                // token khong hop le / het han -> thu session phia duoi
            }
        }

        // 2) Fallback: session cookie (LoginServlet + cac client cu).
        HttpSession session = req.getSession(false);
        if (session != null && session.getAttribute("user") != null) {
            chain.doFilter(request, response);
            return;
        }

        ServletUtils.deny(req, res, HttpServletResponse.SC_UNAUTHORIZED, "Bạn chưa đăng nhập");
    }

    private static boolean isPublic(HttpServletRequest req, String path) {
        if (path.equals("/login")) {
            return true;
        }
        // Auth public endpoints — khong can dang nhap (tru logout can dang nhap de invalidate session).
        if (path.startsWith("/api/auth/") && !path.endsWith("/api/auth/logout")) {
            return true;
        }
        // Swagger UI + OpenAPI spec + webjars — de xem tai lieu API khong can tai khoan.
        if (path.startsWith("/swagger") || path.equals("/openapi.json") || path.startsWith("/webjars/")) {
            return true;
        }
        // Webhook/return cua cong thanh toan: khong the mang Bearer/session — bao mat bang
        // ma gatewayTransactionRef + kiem tra chu ky o tang service.
        if (path.startsWith("/api/payments/webhook") || path.equals("/api/payments/vnpay/return")) {
            return true;
        }
        // Doc duoc kong khai — chi GET; POST/PUT/DELETE van phai dang nhap.
        if ("GET".equalsIgnoreCase(req.getMethod())) {
            for (String prefix : PUBLIC_READ_PREFIXES) {
                if (path.startsWith(prefix)) return true;
            }
        }
        return false;
    }

    static String stripContext(HttpServletRequest req) {
        String uri = req.getRequestURI();
        String ctx = req.getContextPath();
        if (ctx != null && !ctx.isEmpty() && uri.startsWith(ctx)) {
            return uri.substring(ctx.length());
        }
        return uri;
    }
}
