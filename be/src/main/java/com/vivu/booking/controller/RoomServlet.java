package com.vivu.booking.controller;

import com.vivu.booking.dto.request.RoomCreateRequest;
import com.vivu.booking.dto.request.RoomUpdateRequest;
import com.vivu.booking.enums.RoomStatus;
import com.vivu.booking.enums.RoomType;
import com.vivu.booking.service.RoomService;
import com.vivu.booking.service.impl.RoomServiceImpl;
import com.vivu.booking.utils.ServletUtils;
import com.vivu.booking.utils.ValidationUtils;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

/**
 * CRUD phong + upload/xoa anh & video phong (MinIO).
 * Routes:
 * GET    /api/rooms                    -> list (q, type, status, minPrice, maxPrice, capacity, sortBy, sortDir, page, size)
 * POST   /api/rooms                    -> create (JSON body)
 * GET    /api/rooms/{id}               -> get by id (kèm images/videos/media từ bảng room_images)
 * PUT    /api/rooms/{id}               -> update (JSON body)
 * DELETE /api/rooms/{id}               -> soft delete
 * POST   /api/rooms/{id}/images        -> upload media (multipart field "file") — tương thích cũ, nhận ảnh & video
 * POST   /api/rooms/{id}/media         -> upload nhiều media (multipart fields "files" hoặc "file")
 * GET    /api/rooms/{id}/media         -> danh sách media của phòng (ảnh + video)
 * DELETE /api/rooms/{id}/media/{mediaId} -> xoá một media (xoá DB + MinIO object)
 */
@WebServlet(urlPatterns = "/api/rooms/*")
@MultipartConfig(fileSizeThreshold = 1024 * 1024, maxFileSize = 50 * 1024 * 1024, maxRequestSize = 100 * 1024 * 1024)
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
            String path = req.getPathInfo(); // null or "/{id}" or "/{id}/media"
            // GET /{id}/media — danh sách media (ảnh + video)
            if (path != null && path.matches("^/\\d+/media/?$")) {
                Long roomId = parseId(path.substring(0, path.indexOf("/media")));
                var room = roomService.getById(roomId);
                ServletUtils.ok(req, resp, room.getMedia() == null ? java.util.List.of() : room.getMedia());
                return;
            }
            if (path == null || path.equals("/")) {
                RoomType type = parseEnum(req.getParameter("type"), RoomType.class);
                RoomStatus status = parseEnum(req.getParameter("status"), RoomStatus.class);
                String q = req.getParameter("q");
                int page = ServletUtils.parseIntParam(req, "page", 0);
                int size = Math.min(ServletUtils.parseIntParam(req, "size", 20), 100);
                if (page < 0) page = 0;
                if (size <= 0) size = 20;
                // Bộ lọc nâng cao cho màn Home — khoảng giá, sức chứa, sắp xếp
                Long minPrice = parseLong(req.getParameter("minPrice"));
                Long maxPrice = parseLong(req.getParameter("maxPrice"));
                Integer capacity = parseCapacity(req.getParameter("capacity"));
                String sortBy = req.getParameter("sortBy");
                String sortDir = req.getParameter("sortDir");
                var result = roomService.list(type, status, q, minPrice, maxPrice, capacity, page, size, sortBy, sortDir);
                ServletUtils.ok(req, resp, result);
            } else {
                Long id = parseId(path);
                var result = roomService.getById(id);
                ServletUtils.ok(req, resp, result);
            }
        } catch (Exception e) {
            ServletUtils.handleException(req, resp, e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String path = req.getPathInfo();
            // /{id}/images (tương thích cũ) hoặc /{id}/media — upload ảnh & video
            if (path != null && (path.matches("^/\\d+/images/?$") || path.matches("^/\\d+/media/?$"))) {
                boolean legacy = path.contains("/images");
                Long roomId = parseId(path.substring(0, path.indexOf(legacy ? "/images" : "/media")));
                // Cho phép upload nhiều file trong một lần: fields "files", "file"
                Collection<Part> parts = req.getParts();
                List<Part> files = parts.stream().filter(p -> {
                    String name = p.getName();
                    return ("files".equals(name) || "file".equals(name)) && p.getSize() > 0;
                }).toList();
                if (files.isEmpty()) {
                    throw new com.vivu.booking.exception.BusinessException(400, "Thiếu file (field 'files' hoặc 'file' — hỗ trợ nhiều ảnh/video)");
                }
                if (files.size() == 1) {
                    String url = roomService.uploadMedia(roomId, files.get(0));
                    ServletUtils.created(req, resp, java.util.Map.of("url", url));
                } else {
                    var urls = new java.util.ArrayList<String>();
                    for (Part f : files) {
                        try { urls.add(roomService.uploadMedia(roomId, f)); }
                        catch (Exception e) { throw e; }
                    }
                    ServletUtils.created(req, resp, java.util.Map.of("urls", urls));
                }
                return;
            }
            // / — create
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
            String path = req.getPathInfo();
            // DELETE /{id}/media/{mediaId} — xoá một media của phòng
            if (path != null && path.matches("^/\\d+/media/\\d+/?$")) {
                String[] parts = path.replaceFirst("^/", "").split("/");
                Long roomId = Long.parseLong(parts[0]);
                Long mediaId = Long.parseLong(parts[2]);
                roomService.deleteMedia(roomId, mediaId);
                ServletUtils.ok(req, resp, java.util.Map.of("deletedId", mediaId));
                return;
            }
            Long id = parseId(path);
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

    private static Long parseLong(String val) {
        if (val == null || val.isBlank()) return null;
        try { return Long.parseLong(val.trim()); } catch (NumberFormatException e) { return null; }
    }

    private static Integer parseCapacity(String val) {
        if (val == null || val.isBlank()) return null;
        try { return Integer.parseInt(val.trim()); } catch (NumberFormatException e) { return null; }
    }
}
