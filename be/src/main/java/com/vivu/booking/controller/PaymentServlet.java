package com.vivu.booking.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.vivu.booking.exception.BusinessException;
import com.vivu.booking.service.PaymentService;
import com.vivu.booking.service.impl.PaymentServiceImpl;
import com.vivu.booking.utils.JwtUtil;
import com.vivu.booking.utils.ServletUtils;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Map;

/**
 * Thanh toan cho Booking HOLD — dung cong fake-bank (103.216.117.40:8082) hoac VNPay.
 *
 *  POST /api/payments                     — tao giao dich + tra checkoutUrl (can dang nhap, body PaymentRequest)
 *  GET  /api/payments/by-booking/{id}    — payment cua 1 booking cua toi (de poll truoc khi quay lai FE)
 *  GET  /api/payments/{id}               — chi tiet 1 payment cua toi
 *  POST /api/payments/webhook*           — fake-bank goi toi khi khach hoan tat/ huy tren trang checkout
 *                                          body { event: payment.succeeded|payment.failed, payment_id, amount, currency }
 *  GET  /api/payments/vnpay/return       — VNPay return (forward toi {@link VnPayReturnServlet} neu con dung)
 *
 * Lun lay userId tu Bearer / HttpSession nhu BookingServlet/AuthServlet — khong hardcode 1L.
 */
@WebServlet(urlPatterns = {"/api/payments", "/api/payments/*"})
public class PaymentServlet extends HttpServlet {

    private PaymentService paymentService;

    @Override
    public void init() {
        this.paymentService = new PaymentServiceImpl();
    }

    void setPaymentService(PaymentService svc) { this.paymentService = svc; }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        try {
            // Fake-bank webhook tu dong goi toi (khong can auth Bearer).
            if (path != null && (path.equals("/webhook") || path.startsWith("/webhook/"))) {
                String body = ServletUtils.readBodyAsString(req);
                JsonNode root = body == null || body.isBlank() ? null
                        : new com.fasterxml.jackson.databind.ObjectMapper().readTree(body);
                if (root == null) throw new BusinessException(400, "Webhook body trong");
                String event = root.path("event").asText("");
                String paymentRef = root.path("payment_id").asText(
                        root.path("paymentId").asText(root.path("payment_id").asText("")));
                // fallback: gatewayRef co the duoi key khac (merchant_reference, gateway_transaction_ref)
                if (paymentRef == null || paymentRef.isBlank()) {
                    paymentRef = root.path("gateway_transaction_ref").asText(null);
                }
                if (paymentRef == null || paymentRef.isBlank()) {
                    throw new BusinessException(400, "Webhook thieu payment_id");
                }
                boolean success = "payment.succeeded".equals(event);
                boolean failed = "payment.failed".equals(event);
                if (!success && !failed) {
                    // idempotent: tra 200 de cong dung retry nhung khong lam gi
                    ServletUtils.ok(req, resp, Map.of("received", true, "event", event));
                    return;
                }
                try {
                    // signatureVerified=false — fake-bank lab khong co chu ky, nen
                    // PaymentServiceImpl se GOI NGUOC LEN CONG (GET /v1/payments/{id}) de
                    // xac minh status + so tien that. Tin nguyen van webhook = ai biet URL
                    // cung tu xac nhan duoc thanh toan ma khong tra dong nao.
                    var result = paymentService.handleGatewayResult(paymentRef, success, false);
                    ServletUtils.ok(req, resp, Map.of("received", true, "paymentRef", paymentRef,
                            "success", success, "bookingId", result == null ? "" : result.getId()));
                } catch (BusinessException be) {
                    // 4xx = loi nghiep vu co dinh (ma khong ton tai, chu ky sai, booking het han) —
                    // cong retry cung khong khac, tra dung ma de no dung lai.
                    ServletUtils.writeJson(resp, be.getStatus(),
                            Map.of("received", true, "error", String.valueOf(be.getMessage())));
                } catch (Exception e) {
                    // Loi tam thoi (DB/Redis/cong nghet) — tra 5xx de CONG BAN LAI.
                    // Ban cu luon tra 200: mot lan DB glitch la giao dich mat vinh vien,
                    // khong ai biet de doi soat.
                    ServletUtils.writeJson(resp, 503,
                            Map.of("received", false, "retry", true, "error", String.valueOf(e.getMessage())));
                }
                return;
            }

            // Doi soat chu dong — FE goi khi khach quay ve tu trang checkout cua cong
            // (kenh "keo", bu vao cho webhook khong toi duoc BE local). Can dang nhap.
            if (path != null && path.startsWith("/reconcile/")) {
                Long uid = requireAuth(req);
                Long bookingId = parseId(path.substring("/reconcile".length()));
                var result = paymentService.reconcileByBookingId(uid, bookingId);
                ServletUtils.ok(req, resp, result);
                return;
            }

            // Tao giao dich — can dang nhap
            if (path != null && !path.equals("/") && !path.isBlank()) {
                throw new BusinessException(404, "Khong tim thay endpoint POST /api/payments" + path);
            }
            Long uid = requireAuth(req);
            var body = ServletUtils.readBody(req, com.vivu.booking.dto.request.PaymentRequest.class);
            com.vivu.booking.utils.ValidationUtils.validate(body);
            var result = paymentService.createPayment(body, uid, req.getRemoteAddr());
            ServletUtils.created(req, resp, result);
        } catch (Exception e) {
            ServletUtils.handleException(req, resp, e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String path = req.getPathInfo();
            if (path == null || path.equals("/")) {
                throw new BusinessException(404, "GET /api/payments — su dung /by-booking/{id} hoac /{paymentId}");
            }
            Long uid = requireAuth(req);
            if (path.startsWith("/by-booking/")) {
                Long bookingId = Long.parseLong(path.substring("/by-booking/".length()).replaceFirst("^/", "").split("/")[0]);
                var result = paymentService.getByBookingId(uid, bookingId);
                ServletUtils.ok(req, resp, result);
                return;
            }
            Long paymentId = parseId(path);
            var result = paymentService.getById(uid, paymentId);
            ServletUtils.ok(req, resp, result);
        } catch (Exception e) {
            ServletUtils.handleException(req, resp, e);
        }
    }

    private static Long parseId(String pathInfo) {
        if (pathInfo == null || pathInfo.equals("/")) throw new BusinessException(400, "Missing id");
        String s = pathInfo.replaceFirst("^/", "").split("/")[0];
        try { return Long.parseLong(s); } catch (NumberFormatException e) { throw new BusinessException(400, "Invalid id: " + s); }
    }

    private Long requireAuth(HttpServletRequest req) {
        String header = req.getHeader("Authorization");
        if (header != null && header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            String token = header.substring(7).trim();
            JwtUtil.Claims claims = JwtUtil.parse(token);
            if (!"access".equals(claims.getType())) throw new BusinessException(401, "Token khong phai access token");
            return claims.getUserId();
        }
        HttpSession sess = req.getSession(false);
        if (sess != null) {
            Object a = sess.getAttribute("user");
            if (a instanceof com.vivu.booking.dto.response.AuthTokenResponse.UserSummary us && us.getId() != null) return us.getId();
            if (a instanceof com.vivu.booking.dto.response.UsersLoginResponse ul && ul.getId() != null) return ul.getId();
        }
        throw new BusinessException(401, "Chua dang nhap — gui Authorization: Bearer <accessToken>");
    }
}
