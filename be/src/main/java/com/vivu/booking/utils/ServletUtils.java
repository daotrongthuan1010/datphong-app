package com.vivu.booking.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vivu.booking.common.ApiResponse;
import com.vivu.booking.exception.BusinessException;
import com.vivu.booking.exception.ValidationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class ServletUtils {
    private static final Logger log = LoggerFactory.getLogger(ServletUtils.class);
    private static final ObjectMapper MAPPER = JsonUtils.mapper();

    private ServletUtils() {
    }

    public static String requestId(HttpServletRequest req) {
        Object v = req.getAttribute("X-Request-Id");
        return v != null ? v.toString() : req.getHeader("X-Request-Id");
    }

    public static <T> T readBody(HttpServletRequest req, Class<T> type) throws IOException {
        return MAPPER.readValue(req.getInputStream(), type);
    }

    public static String readBodyAsString(HttpServletRequest req) throws IOException {
        return new String(req.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }

    public static void writeJson(HttpServletResponse resp, int status, Object body) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        String rid = resp.getHeader("X-Request-Id");
        if (body instanceof ApiResponse<?> ar && rid != null && ar.getRequestId() == null) {
            ar.setRequestId(rid);
        }
        resp.getWriter().write(MAPPER.writeValueAsString(body));
    }

    public static void ok(HttpServletRequest req, HttpServletResponse resp, Object data) throws IOException {
        ApiResponse<?> ar = ApiResponse.ok(data);
        ar.setRequestId(requestId(req));
        ar.setTimestamp(System.currentTimeMillis());
        writeJson(resp, 200, ar);
    }

    public static void created(HttpServletRequest req, HttpServletResponse resp, Object data) throws IOException {
        ApiResponse<?> ar = ApiResponse.ok("Created", data);
        ar.setRequestId(requestId(req));
        writeJson(resp, 201, ar);
    }

    public static void handleException(HttpServletRequest req, HttpServletResponse resp, Exception e) throws IOException {
        String rid = requestId(req);
        if (e instanceof ValidationException ve) {
            var body = Map.of("success", false, "message", ve.getMessage(), "errors", ve.getErrors(),
                    "requestId", rid != null ? rid : "", "timestamp", System.currentTimeMillis());
            writeJson(resp, ve.getStatus(), body);
        } else if (e instanceof BusinessException be) {
            var body = ApiResponse.fail(be.getMessage());
            body.setRequestId(rid);
            writeJson(resp, be.getStatus(), body);
        } else {
            log.error("Unhandled error rid={}", rid, e);
            var body = ApiResponse.fail("Internal server error");
            body.setRequestId(rid);
            writeJson(resp, 500, body);
        }
    }

    public static int parseIntParam(HttpServletRequest req, String name, int def) {
        String v = req.getParameter(name);
        if (v == null || v.isBlank()) return def;
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException ex) {
            return def;
        }
    }
    public static void error(HttpServletRequest req,HttpServletResponse resp, Map<String,String> data) throws IOException {
        ApiResponse<?> ar = ApiResponse.fails("Error", data);
        ar.setRequestId(requestId(req));
        writeJson(resp, 400, ar);
    }
}
