package com.vivu.booking.filter;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebFilter("/*")
public class AuthenFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req0 = (HttpServletRequest) request;
        // Preflight CORS (OPTIONS) KHONG mang cookie/Authorization. Neu chan o day (401)
        // thi response khong co header Access-Control-* -> trinh bao loi "CORS error".
        // Cho di tiep de CorsFilter gan header. Lam vay ket qua KHONG phu thuoc thu tu
        // chay giua AuthenFilter va CorsFilter (von do container quyet dinh, khac nhau
        // giua cac may/ phien ban Tomcat).
        if ("OPTIONS".equalsIgnoreCase(req0.getMethod())) {
            chain.doFilter(request, response);
            return;
        }
        String path = req0.getRequestURI();
        if (path.equals("/login")) {
            chain.doFilter(request, response);
            return;
        }
        // Auth public endpoints - khong can session (tru logout can dang nhap)
        if (path.contains("/api/auth/") && !path.endsWith("/api/auth/logout")) {
            chain.doFilter(request, response);
            return;
        }

        // Public browse: xem danh sach/phong/health + review khong can dang nhap,
        // chi GET moi duoc mo, POST/PUT/DELETE van giu nguyen session check.
        HttpServletRequest getReq = (HttpServletRequest) request;
        if ("GET".equalsIgnoreCase(getReq.getMethod())
                && (path.startsWith("/api/rooms") || path.startsWith("/api/health") || path.startsWith("/api/reviews"))) {
            chain.doFilter(request, response);
            return;
        }
        // Swagger UI + OpenAPI spec + webjars — public cho moi nguoi xem tai lieu API
        if (path.startsWith("/swagger") || path.equals("/openapi.json") || path.startsWith("/webjars/")) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        HttpSession session = req.getSession();
        if (session != null && session.getAttribute("user") != null) {
            chain.doFilter(request, response);
        } else {
            res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Bạn chưa đăng nhập");
        }
    }
}
