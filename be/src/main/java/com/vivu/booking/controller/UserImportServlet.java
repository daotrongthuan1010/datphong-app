package com.vivu.booking.controller;

import com.vivu.booking.dao.RoleDao;
import com.vivu.booking.dao.UsersDao;
import com.vivu.booking.service.UserImportService;
import com.vivu.booking.service.impl.UserImportServiceImpl;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import java.io.IOException;

@WebServlet("/api/users/import-excel")
@MultipartConfig
public class UserImportServlet extends HttpServlet {

    private final UserImportService userImportService =
            new UserImportServiceImpl(
                    new UsersDao(),
                    new RoleDao()
            );

    @Override
    protected void doPost(
            HttpServletRequest req,
            HttpServletResponse resp
    ) throws ServletException, IOException {

        resp.setContentType("application/json;charset=UTF-8");

        try {

            Part file = req.getPart("file");

            if (file == null || file.getSize() == 0) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write(
                        "{\"message\":\"File không được để trống\"}"
                );
                return;
            }

            userImportService.importExcel(
                    file.getInputStream()
            );

            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(
                    "{\"message\":\"Import thành công\"}"
            );

        } catch (Exception e) {

            e.printStackTrace();

            resp.setStatus(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR
            );

            resp.getWriter().write(
                    "{\"message\":\"Import thất bại\"}"
            );
        }
    }
}