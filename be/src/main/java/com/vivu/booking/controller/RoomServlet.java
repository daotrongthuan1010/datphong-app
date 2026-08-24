package com.vivu.booking.controller;

import com.vivu.booking.dto.request.RoomCreateRequest;
import com.vivu.booking.dto.request.RoomUpdateRequest;
import com.vivu.booking.enums.RoomStatus;
import com.vivu.booking.enums.RoomType;
import com.vivu.booking.service.RoomService;
import com.vivu.booking.service.impl.RoomServiceImpl;
import com.vivu.booking.utils.ServletUtils;
import com.vivu.booking.utils.ValidationUtils;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * CRUD demo cho sinh viên: minh họa Servlet trả JSON + Service/DAO/Mapper/Validation.
 * Routes:
 * GET    /api/rooms              -> list (query: type, status, q, page, size)
 * POST   /api/rooms              -> create (JSON body)
 * GET    /api/rooms/{id}         -> get by id
 * PUT    /api/rooms/{id}         -> update (JSON body)
 * DELETE /api/rooms/{id}         -> soft delete
 */
@WebServlet(urlPatterns = "/api/rooms/*")
public class RoomServlet extends HttpServlet {

    private RoomService roomService;

    @Override
    public void init() {
        this.roomService = new RoomServiceImpl();
    }

    // For testing / DI
    public void setRoomService(RoomService svc) {
        this.roomService = svc;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String path = req.getPathInfo(); // null or "/{id}"
            if (path == null || path.equals("/")) {
                RoomType type = parseEnum(req.getParameter("type"), RoomType.class);
                RoomStatus status = parseEnum(req.getParameter("status"), RoomStatus.class);
                String q = req.getParameter("q");
                int page = ServletUtils.parseIntParam(req, "page", 0);
                int size = Math.min(ServletUtils.parseIntParam(req, "size", 20), 100);
                if (page < 0) page = 0;
                if (size <= 0) size = 20;
                var result = roomService.list(type, status, q, page, size);
                ServletUtils.ok(req, resp, result);
            } else {
                Long id = parseId(path);
                ServletUtils.ok(req, resp, roomService.getById(id));
            }
        } catch (Exception e) {
            ServletUtils.handleException(req, resp, e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            RoomCreateRequest body = ServletUtils.readBody(req, RoomCreateRequest.class);
            ValidationUtils.validate(body);
            var created = roomService.create(body);
            ServletUtils.created(req, resp, created);
        } catch (Exception e) {
            ServletUtils.handleException(req, resp, e);
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            Long id = parseId(req.getPathInfo());
            RoomUpdateRequest body = ServletUtils.readBody(req, RoomUpdateRequest.class);
            var updated = roomService.update(id, body);
            ServletUtils.ok(req, resp, updated);
        } catch (Exception e) {
            ServletUtils.handleException(req, resp, e);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            Long id = parseId(req.getPathInfo());
            roomService.delete(id);
            ServletUtils.ok(req, resp, java.util.Map.of("deletedId", id));
        } catch (Exception e) {
            ServletUtils.handleException(req, resp, e);
        }
    }

    private static Long parseId(String pathInfo) {
        if (pathInfo == null || pathInfo.equals("/"))
            throw new com.vivu.booking.exception.BusinessException(400, "Missing id in path");
        String s = pathInfo.replaceFirst("^/", "").split("/")[0];
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            throw new com.vivu.booking.exception.BusinessException(400, "Invalid id: " + s);
        }
    }

    private static <E extends Enum<E>> E parseEnum(String val, Class<E> type) {
        if (val == null || val.isBlank()) return null;
        try {
            return Enum.valueOf(type, val.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new com.vivu.booking.exception.BusinessException(400, "Invalid " + type.getSimpleName() + ": " + val);
        }
    }
}
