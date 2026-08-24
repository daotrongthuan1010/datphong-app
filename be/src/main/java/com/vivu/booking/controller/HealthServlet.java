package com.vivu.booking.controller;

import com.vivu.booking.common.ApiResponse;
import com.vivu.booking.utils.ServletUtils;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

@WebServlet(urlPatterns = "/api/health")
public class HealthServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        ApiResponse<?> body = ApiResponse.ok(Map.of(
                "status", "UP",
                "service", "booking-app-be",
                "version", "1.0.0-SNAPSHOT"
        ));
        body.setRequestId(ServletUtils.requestId(req));
        ServletUtils.writeJson(resp, 200, body);
    }
}
