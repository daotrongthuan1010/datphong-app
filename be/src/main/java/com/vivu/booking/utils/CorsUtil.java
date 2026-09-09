package com.vivu.booking.utils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Gan header CORS — tach rieng de <b>bat ky filter nao</b> cung goi duoc, khong phu thuoc
 * thu tu chay giua cac {@code @WebFilter}.
 *
 * <p>Van de cu: {@code CorsFilter} chay SAU {@code AuthenFilter}/{@code AuthorFilter}
 * (thu tu filter cung url-pattern do container quyet dinh), nen response 401/403 do 2 filter
 * do tu chan khong co {@code Access-Control-Allow-Origin}. Trinh duyet bao "CORS error",
 * axios khong thay {@code err.response.status} → interceptor refresh-token khong chay,
 * nguoi dung khong duoc dang xuat tu dong.
 *
 * <p>FE dung {@code withCredentials:true} (gui JSESSIONID + Bearer) nen khong duoc tra "*".
 * Phan xa (echo) lai Origin cua request de ho tro localhost, IP LAN, hostname...
 */
public final class CorsUtil {

    private CorsUtil() {
    }

    public static void apply(HttpServletRequest req, HttpServletResponse res) {
        String origin = req.getHeader("Origin");
        // Chi chap nhan gia tri hop le (scheme://host[:port], khong khoang trang / dau "/")
        // de tranh header injection.
        boolean validOrigin = origin != null && origin.matches("^https?://[^/\\s]+$");
        if (validOrigin) {
            res.setHeader("Access-Control-Allow-Origin", origin);
            res.setHeader("Access-Control-Allow-Credentials", "true");
            res.addHeader("Vary", "Origin");
        } else {
            // Khong co Origin (same-origin / non-browser) -> cho phep tat ca, khong credentials.
            res.setHeader("Access-Control-Allow-Origin", "*");
        }
        res.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, PATCH, DELETE, OPTIONS");
        res.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Request-Id");
        res.setHeader("Access-Control-Expose-Headers", "X-Request-Id");
    }
}
