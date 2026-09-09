package com.vivu.booking.controller;

import com.vivu.booking.service.PaymentService;
import com.vivu.booking.service.impl.PaymentServiceImpl;
import com.vivu.booking.utils.ServletUtils;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

/**
 * VNPay return — GET /api/payments/vnpay/return?vnp_TxnRef=...&vnp_ResponseCode=...&vnp_SecureHash=...
 *
 * <h2>Return URL khong phai la nguon chan ly</h2>
 * <p>
 * Day la redirect cua <b>trinh duyet khach</b>. Khach co the tu mo lai URL nay, sua tham so,
 * hoac don gian la khong quay lai (dong tab). Vi vay endpoint nay chi lam dung 2 viec:
 * <ol>
 *   <li>Xac minh chu ky {@code vnp_SecureHash} — day moi la tin hieu that.</li>
 *   <li>Nhoi qua {@link PaymentService#handleGatewayResult(String, boolean, boolean)} voi
 *       {@code signatureVerified=true} de <b>mot luong code duy nhat</b> xu ly tien + phong.</li>
 * </ol>
 *
 * <h2>Ba loi cua ban cu (giu lai de hoc)</h2>
 * <ul>
 *   <li><b>Hai luong code:</b> ban cu tu goi {@code bookingService.confirm(...)} roi moi cap nhat
 *       Payment — khac voi luong webhook. Mot nghiep vu co hai noi quyet dinh thi som muon
 *       cung lech nhau.</li>
 *   <li><b>Nuot loi:</b> {@code catch (Exception ignore)} quanh viec cap nhat Payment. Neu buoc
 *       do fail thi booking da CONFIRMED ma Payment van PENDING — tien va so sach lech nhau,
 *       khong co log nao de phat hien.</li>
 *   <li><b>LazyInitializationException:</b> {@code paymentDao.findById()} tra entity da detach
 *       (Session dong), sau do doc {@code payment.getBooking().getId()} → loi, bi catch chung
 *       thanh 500. Ban moi lay bookingId ben trong transaction cua service.</li>
 * </ul>
 */
@WebServlet("/api/payments/vnpay/return")
public class VnPayReturnServlet extends HttpServlet {

    private final PaymentService paymentService = new PaymentServiceImpl();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            Map<String, String> params = new java.util.HashMap<>();
            req.getParameterMap().forEach((k, v) -> { if (v.length > 0) params.put(k, v[0]); });

            String vnpSecureHash = params.get("vnp_SecureHash");
            String responseCode = params.get("vnp_ResponseCode");
            String txnRef = params.get("vnp_TxnRef");

            if (txnRef == null || txnRef.isBlank()) {
                ServletUtils.writeJson(resp, 400, Map.of("success", false, "message", "Thieu vnp_TxnRef"));
                return;
            }
            // Chu ky la tin hieu duy nhat dang tin tu return URL — sai chu ky thi bo qua tat ca.
            boolean valid = com.vivu.booking.payment.gateway.vnpay.VnPayUtil
                    .verifyReturnParams(params, vnpSecureHash);
            if (!valid) {
                ServletUtils.writeJson(resp, 400, Map.of("success", false, "message", "Chu ky VNPay khong hop le"));
                return;
            }

            boolean success = "00".equals(responseCode);
            // Mot luong code duy nhat voi webhook: Payment + Booking cung transaction, idempotent.
            var booking = paymentService.handleGatewayResult(txnRef, success, true);
            if (success) {
                ServletUtils.ok(req, resp, Map.of("success", true,
                        "message", "Thanh toan thanh cong",
                        "booking", booking == null ? Map.of() : booking));
            } else {
                ServletUtils.ok(req, resp, Map.of("success", false,
                        "message", "Thanh toan that bai ma=" + responseCode));
            }
        } catch (Exception e) {
            ServletUtils.handleException(req, resp, e);
        }
    }
}
