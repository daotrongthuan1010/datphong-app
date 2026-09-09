package com.vivu.booking.filter;

import com.vivu.booking.utils.CorsUtil;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Gan header CORS cho MOI response. Filter nay duoc sap chay DAU TIEN trong
 * WEB-INF/web.xml — neu khong, 401/403 do AuthenFilter/AuthorFilter tra som
 * se khong co Access-Control-Allow-Origin va trinh duyet bao "CORS error".
 *
 * <p>Dau loc khong khai bao @WebFilter (thu tu do container quyet dinh),
 * ma khai bao tường minh trong web.xml.
 */
public class CorsFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpReq = (HttpServletRequest) req;
        HttpServletResponse httpRes = (HttpServletResponse) res;

        CorsUtil.apply(httpReq, httpRes);
        if ("OPTIONS".equalsIgnoreCase(httpReq.getMethod())) {
            // Preflight khong mang cookie/Authorization — dung tai day, khong xuong auth.
            httpRes.setStatus(204);
            return;
        }
        chain.doFilter(req, res);
    }
}
