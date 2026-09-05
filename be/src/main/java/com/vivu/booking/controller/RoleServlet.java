package com.vivu.booking.controller;

import com.vivu.booking.dao.RoleDao;
import com.vivu.booking.utils.ServletUtils;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

/**
 * GET /api/roles -> danh sach role (id, code, name) cho FE admin tao/sua user chon roleId.
 */
@WebServlet(urlPatterns = "/api/roles")
public class RoleServlet extends HttpServlet {

    private final RoleDao roleDao = new RoleDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            var roles = roleDao.findAll(0, 100);
            var content = roles.stream()
                    .map(r -> Map.of(
                            "id", r.getId(),
                            "code", r.getCode() != null ? r.getCode() : "",
                            "name", r.getName() != null ? r.getName() : ""))
                    .toList();
            ServletUtils.ok(req, resp, content);
        } catch (Exception e) {
            ServletUtils.handleException(req, resp, e);
        }
    }
}
