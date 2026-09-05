package com.vivu.booking.filter;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebFilter("/*")
public class CorsFilter implements Filter {
    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletResponse httpRes = (HttpServletResponse) res;
        HttpServletRequest httpReq = (HttpServletRequest) req;

        // FE dung withCredentials:true (gui JSESSIONID + Bearer) nen khong duoc tra "*".
        // Phan xa (echo) lai Origin cua request de ho tro truy cap qua localhost, IP LAN,
        // hostname... Chi chap nhan gia tri hop le (scheme://host[:port], khong chua khoang
        // trang / dau "/") de tranh header injection.
        String origin = httpReq.getHeader("Origin");
        boolean validOrigin = origin != null && origin.matches("^https?://[^/\\s]+$");
        if (validOrigin) {
            httpRes.setHeader("Access-Control-Allow-Origin", origin);
            httpRes.setHeader("Access-Control-Allow-Credentials", "true");
            httpRes.setHeader("Vary", "Origin");
        } else {
            // Khong co Origin (same-origin / non-browser) -> cho phep tat ca, khong credentials.
            httpRes.setHeader("Access-Control-Allow-Origin", "*");
        }
        httpRes.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, PATCH, DELETE, OPTIONS");
        httpRes.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Request-Id");
        httpRes.setHeader("Access-Control-Expose-Headers", "X-Request-Id");
        if ("OPTIONS".equalsIgnoreCase(httpReq.getMethod())) {
            httpRes.setStatus(204);
            return;
        }
        chain.doFilter(req, res);
    }
}
