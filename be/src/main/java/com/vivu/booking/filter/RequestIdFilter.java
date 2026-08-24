package com.vivu.booking.filter;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

@WebFilter("/*")
public class RequestIdFilter implements Filter {
    public static final String REQ_ID = "X-Request-Id";

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpReq = (HttpServletRequest) req;
        HttpServletResponse httpRes = (HttpServletResponse) res;
        String rid = httpReq.getHeader(REQ_ID);
        if (rid == null || rid.isBlank()) rid = UUID.randomUUID().toString();
        httpRes.setHeader(REQ_ID, rid);
        req.setAttribute(REQ_ID, rid);
        chain.doFilter(req, res);
    }
}
