package com.vivu.booking.filter;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


public class AuthorFilter implements Filter {
    private final Map<String, Set<String>> permissions = new HashMap<>();
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        permissions.put("/api/rooms/*",Set.of("ADMIN"));
        permissions.put("/api/books/*",Set.of("ADMIN"));
        permissions.put("/api/users",Set.of("ADMIN"));
    }
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        String uri = req.getRequestURI();
        if(uri.equals("/login")){
            chain.doFilter(request, response);
            return;
        }
        // Cùng lý do AuthenFilter: API Public trong AuthServlet chưa có session/role,
        // phải bỏ qua bước check role ở đây, trừ logout.
        if (uri.contains("/api/auth/") && !uri.endsWith("/api/auth/logout")) {
            chain.doFilter(request, response);
            return;
        }
        HttpSession session = req.getSession(false);
        if(session == null){
            res.sendError(HttpServletResponse.SC_UNAUTHORIZED,"chưa đăng nhập");
            return;
        }
        // /api/auth/logout chỉ cần "đã đăng nhập", không cần role cụ thể nào,
        // và path này vốn không có trong map "permissions" nên phải cho qua ở đây.
        if (uri.endsWith("/api/auth/logout")) {
            chain.doFilter(request, response);
            return;
        }
        // FIX lỗi có sẵn: session "role" thực chất là Set<String> (1 user có thể
        // giữ nhiều role - xem LoginServlet/AuthServlet), ép kiểu (String) như code
        // cũ sẽ ném ClassCastException (lỗi 500) với MỌI request đi qua đây, không
        // riêng API mới. Đọc lại cho đúng kiểu, vẫn nhận String đơn nếu có.
        Object roleAttr = session.getAttribute("role");
        Set<String> userRoles;
        if (roleAttr instanceof Set<?> set) {
            userRoles = new java.util.HashSet<>();
            for (Object o : set) userRoles.add(String.valueOf(o));
        } else if (roleAttr instanceof String s) {
            userRoles = Set.of(s);
        } else {
            userRoles = Set.of();
        }
        if (userRoles.isEmpty()) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN,"Không xác đinh được quyền");
            return;
        }
        String path=req.getServletPath();
        Set<String> allowedRoles=permissions.get(path);
        if(allowedRoles == null){
            res.sendError(HttpServletResponse.SC_FORBIDDEN,"URL chưa được phân quyền");
            return;
        }
        boolean hasAccess = userRoles.stream().anyMatch(allowedRoles::contains);
        if(hasAccess){
            chain.doFilter(request, response);
            return;
        }
        res.sendError(HttpServletResponse.SC_FORBIDDEN,"Bạn không có quyền truy cập");
    }
}
