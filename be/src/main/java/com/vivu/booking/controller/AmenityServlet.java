package com.vivu.booking.controller;

import com.vivu.booking.dao.AmenityDao;
import com.vivu.booking.dto.response.AmenityResponse;
import com.vivu.booking.entity.Amenity;
import com.vivu.booking.utils.ServletUtils;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

/**
 * Danh muc tien nghi — FE can danh sach nay de
 *   (1) hien tien nghi that cua phong trong RoomDetail,
 *   (2) cho admin chon tien nghi khi tao/sua phong (multi-select, khong hardcode mang ten trong FE).
 *
 * GET /api/amenities  -> List<AmenityResponse> (public, chi doc)
 */
@WebServlet("/api/amenities")
public class AmenityServlet extends HttpServlet {

    private final AmenityDao amenityDao = new AmenityDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            int size = Math.min(ServletUtils.parseIntParam(req, "size", 100), 200);
            int page = Math.max(ServletUtils.parseIntParam(req, "page", 0), 0);
            List<AmenityResponse> body = amenityDao.findAll(page, size).stream()
                    .map(AmenityResponse::from)
                    .toList();
            ServletUtils.ok(req, resp, body);
        } catch (Exception e) {
            ServletUtils.handleException(req, resp, e);
        }
    }

    /** Ngchan ghi tai day — tien nghi duoc quan ly qua admin CRUD (chua lam trong dot nay). */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        ServletUtils.writeJson(resp, 405, java.util.Map.of(
                "success", false, "message", "GET /api/amenities moi duoc ho tro"));
    }
}
