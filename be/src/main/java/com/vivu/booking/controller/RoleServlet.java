package com.vivu.booking.controller;

import com.vivu.booking.dto.request.RoleCreateRequest;
import com.vivu.booking.dto.request.RoleUpdateRequest;
import com.vivu.booking.dto.response.RoleResponse;
import com.vivu.booking.exception.BusinessException;
import com.vivu.booking.service.RoleService;
import com.vivu.booking.service.impl.RoleServiceImpl;
import com.vivu.booking.utils.ServletUtils;
import com.vivu.booking.utils.ValidationUtils;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;


@WebServlet(urlPatterns = "/api/roles/*")
public class RoleServlet extends HttpServlet {

    private final RoleService roleService = new RoleServiceImpl();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            Long id = parseId(req.getPathInfo());
            if (id == null) {
                int page = ServletUtils.parseIntParam(req, "page", 0);
                int size = Math.min(ServletUtils.parseIntParam(req, "size", 20), 100);
                if (page < 0) page = 0;
                if (size <= 0) size = 20;
                List<RoleResponse> roles = roleService.list(page, size);
                ServletUtils.ok(req, resp, roles);
            } else {
                RoleResponse role = roleService.getById(id);
                ServletUtils.ok(req, resp, role);
            }
        } catch (Exception e) {
            ServletUtils.handleException(req, resp, e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            RoleCreateRequest body = ServletUtils.readBody(req, RoleCreateRequest.class);
            ValidationUtils.validate(body);
            RoleResponse created = roleService.create(body);
            ServletUtils.created(req, resp, created);
        } catch (Exception e) {
            ServletUtils.handleException(req, resp, e);
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            Long id = parseId(req.getPathInfo());
            if (id == null) {
                throw new BusinessException(400, "Thiếu id role trên URL (VD: /api/roles/5)");
            }
            RoleUpdateRequest body = ServletUtils.readBody(req, RoleUpdateRequest.class);
            ValidationUtils.validate(body);
            RoleResponse updated = roleService.update(id, body);
            ServletUtils.ok(req, resp, updated);
        } catch (Exception e) {
            ServletUtils.handleException(req, resp, e);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            Long id = parseId(req.getPathInfo());
            if (id == null) {
                throw new BusinessException(400, "Thiếu id role trên URL (VD: /api/roles/5)");
            }
            roleService.delete(id);
            ServletUtils.ok(req, resp, java.util.Map.of("message", "Đã xoá role"));
        } catch (Exception e) {
            ServletUtils.handleException(req, resp, e);
        }
    }

    private Long parseId(String pathInfo) {
        if (pathInfo == null || pathInfo.equals("/")) return null;
        try {
            return Long.parseLong(pathInfo.substring(1));
        } catch (NumberFormatException e) {
            throw new BusinessException(400, "id role không hợp lệ: " + pathInfo);
        }
    }
}
