package com.vivu.booking.controller;

import com.vivu.booking.enums.RankNameType;
import com.vivu.booking.service.LoyaltyRankService;
import com.vivu.booking.service.impl.LoyaltyRankServiceImp;
import com.vivu.booking.utils.JwtUtil;
import com.vivu.booking.utils.ServletUtils;
import com.vivu.booking.exception.BusinessException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebServlet(urlPatterns = "/api/loyalty/*")
public class LoyaltyRankServlet extends HttpServlet {

    private LoyaltyRankService loyaltyRankService;

    @Override
    public void init() {
        this.loyaltyRankService = new LoyaltyRankServiceImp();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String path = req.getPathInfo(); // null, "/", "/ranks", "/ranks/GOLD", "/me", "/me/history"
            if (path == null || path.equals("/") || path.equals("/ranks")) {
                int page = ServletUtils.parseIntParam(req, "page", 0);
                int size = Math.min(ServletUtils.parseIntParam(req, "size", 20), 100);
                if (page < 0) page = 0;
                if (size <= 0) size = 20;
                var result = loyaltyRankService.list(page, size);
                ServletUtils.ok(req, resp, result);
                return;
            }
            if (path.equals("/me")) {
                Long uid = requireAuth(req);
                var profile = loyaltyRankService.getMyProfile(uid);
                ServletUtils.ok(req, resp, profile);
                return;
            }
            if (path.equals("/me/history")) {
                Long uid = requireAuth(req);
                int page = ServletUtils.parseIntParam(req, "page", 0);
                int size = Math.min(ServletUtils.parseIntParam(req, "size", 20), 100);
                if (page < 0) page = 0;
                if (size <= 0) size = 20;
                var result = loyaltyRankService.getPointHistory(uid, page, size);
                ServletUtils.ok(req, resp, result);
                return;
            }
            if (path.startsWith("/ranks/")) {
                String nameStr = path.substring("/ranks/".length()).replaceAll("/", "").trim();
                if (nameStr.isEmpty()) throw new BusinessException(400, "Thiếu tên hạng");
                RankNameType name = parseEnum(nameStr, RankNameType.class);
                var rank = loyaltyRankService.getByName(name);
                ServletUtils.ok(req, resp, rank);
                return;
            }
            throw new BusinessException(404, "Không tìm thấy endpoint: /api/loyalty" + path);
        } catch (Exception e) {
            ServletUtils.handleException(req, resp, e);
        }
    }

    private Long requireAuth(HttpServletRequest req) {
        String header = req.getHeader("Authorization");
        if (header != null && header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            String token = header.substring(7).trim();
            JwtUtil.Claims claims = JwtUtil.parse(token);
            if (!"access".equals(claims.getType())) throw new BusinessException(401, "Token không phải access token");
            return claims.getUserId();
        }
        HttpSession sess = req.getSession(false);
        if (sess != null) {
            Object userAttr = sess.getAttribute("user");
            if (userAttr instanceof com.vivu.booking.dto.response.AuthTokenResponse.UserSummary us && us.getId() != null) return us.getId();
            if (userAttr instanceof com.vivu.booking.dto.response.UsersLoginResponse ul && ul.getId() != null) return ul.getId();
        }
        throw new BusinessException(401, "Chưa đăng nhập");
    }

    private static <E extends Enum<E>> E parseEnum(String val, Class<E> type) {
        if (val == null || val.isBlank()) return null;
        try {
            return Enum.valueOf(type, val.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(400, "Invalid " + type.getSimpleName() + ": " + val);
        }
    }
}
