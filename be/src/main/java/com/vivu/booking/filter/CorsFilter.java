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
        // withCredentials:true (FE gui JSESSIONID) can co dinh origin + allow credentials.
        String origin = httpReq.getHeader("Origin");
        String allowOrigin = origin != null && origin.matches("^https?://(localhost|127\\.0\\.0\\.1)(:\\d+)?$") ? origin : "*";
        httpRes.setHeader("Access-Control-Allow-Origin", allowOrigin);
        if (allowOrigin.startsWith("http")) {
            httpRes.setHeader("Access-Control-Allow-Credentials", "true");
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
