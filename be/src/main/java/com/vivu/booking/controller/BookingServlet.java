package com.vivu.booking.controller;

import com.vivu.booking.dto.request.BookingCreateRequest;
import com.vivu.booking.exception.BusinessException;
import com.vivu.booking.service.BookingService;
import com.vivu.booking.service.impl.BookingServiceImpl;
import com.vivu.booking.utils.JwtUtil;
import com.vivu.booking.utils.ServletUtils;
import com.vivu.booking.utils.ValidationUtils;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Map;

/**
 * Booking API (tuong tu /api/auth — gom nhieu pathCon duoi 1 Servlet):
 *   POST /api/bookings                  — tao booking (can dang nhap, lay userId tu session / Bearer token)
 *   GET  /api/bookings                  — danh sach booking cua toi (page, size)
 *   GET  /api/bookings/{id}             — chi tiet 1 booking cua toi
 *   POST /api/bookings/{id}/cancel      — huy booking cua toi
 *
 * Dung chung co che auth voi AuthServlet/HostProfile — lay userId tu session/bearer (xem requireAuth).
 */
@WebServlet(urlPatterns = "/api/bookings/*")
public class BookingServlet extends HttpServlet {

    private BookingService bookingService;

    @Override
    public void init() {
        this.bookingService = new BookingServiceImpl();
    }

    public void setBookingService(BookingService svc) {
        this.bookingService = svc;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo(); // null, "/" hay "/{id}/cancel"
        try {
            if (path != null && path.endsWith("/cancel")) {
                Long id = parseId(path.replace("/cancel", "").trim());
                Long uid = requireAuth(req);
                var result = bookingService.cancel(uid, id);
                ServletUtils.ok(req, resp, result);
                return;
            }
            if (path != null && !path.equals("/") && !path.isBlank()) {
                throw new BusinessException(404, "Khong tim thay endpoint POST " + path);
            }
            Long uid = requireAuth(req);
            BookingCreateRequest body = ServletUtils.readBody(req, BookingCreateRequest.class);
            ValidationUtils.validate(body);
            var created = bookingService.create(uid, body);
            ServletUtils.created(req, resp, created);
        } catch (Exception e) {
            ServletUtils.handleException(req, resp, e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String path = req.getPathInfo();
            Long uid = requireAuth(req);
            if (path == null || path.equals("/")) {
                int page = ServletUtils.parseIntParam(req, "page", 0);
                int size = Math.min(ServletUtils.parseIntParam(req, "size", 10), 100);
                if (page < 0) page = 0;
                if (size <= 0) size = 10;
                var result = bookingService.listByUser(uid, page, size);
                ServletUtils.ok(req, resp, result);
                return;
            }
            // GET /api/bookings/{id}
            Long id = parseId(path);
            var result = bookingService.getById(uid, id);
            ServletUtils.ok(req, resp, result);
        } catch (Exception e) {
            ServletUtils.handleException(req, resp, e);
        }
    }

    private static Long parseId(String pathInfo) {
        if (pathInfo == null || pathInfo.equals("/"))
            throw new BusinessException(400, "Missing id in path");
        String s = pathInfo.replaceFirst("^/", "").split("/")[0];
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            throw new BusinessException(400, "Invalid id: " + s);
        }
    }

    private Long requireAuth(HttpServletRequest req) {
        // 1) Bearer access token
        String header = req.getHeader("Authorization");
        if (header != null && header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            String token = header.substring(7).trim();
            JwtUtil.Claims claims = JwtUtil.parse(token);
            if (!"access".equals(claims.getType())) {
                throw new BusinessException(401, "Token khong phai access token");
            }
            return claims.getUserId();
        }
        // 2) HttpSession (tu /api/auth/login — session duoc set tu ServletUtils/cookie)
        HttpSession sess = req.getSession(false);
        if (sess != null) {
            Object userAttr = sess.getAttribute("user");
            if (userAttr instanceof com.vivu.booking.dto.response.AuthTokenResponse.UserSummary us && us.getId() != null) {
                return us.getId();
            }
            if (userAttr instanceof com.vivu.booking.dto.response.UsersLoginResponse ul && ul.getId() != null) {
                return ul.getId();
            }
        }
        throw new BusinessException(401, "Chua dang nhap — vui long dang nhap truoc khi dat phong");
    }
}
