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
    }
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        String uri = req.getRequestURI();
        if(uri.equals("/login")){
            chain.doFilter(request, response);
        }
        HttpSession session = req.getSession(false);
        if(session == null){
            res.sendError(HttpServletResponse.SC_UNAUTHORIZED,"chưa đăng nhập");
            return;
        }
        String role = (String) session.getAttribute("role");
        if(role == null){
            res.sendError(HttpServletResponse.SC_FORBIDDEN,"Không xác đinh được quyền");
            return;
        }
        String path=req.getServletPath();
        Set<String> allowedRoles=permissions.get(path);
        if(allowedRoles == null){
            res.sendError(HttpServletResponse.SC_FORBIDDEN,"URL chưa được phân quyền");
            return;
        }
        if(allowedRoles.contains(role)){
            chain.doFilter(request, response);
            return;
        }
        res.sendError(HttpServletResponse.SC_FORBIDDEN,"Bạn không có quyền truy cập");
    }
}
