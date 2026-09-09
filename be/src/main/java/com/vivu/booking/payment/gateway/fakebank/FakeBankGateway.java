package com.vivu.booking.payment.gateway.fakebank;

import com.fasterxml.jackson.databind.JsonNode;
import com.vivu.booking.entity.Booking;
import com.vivu.booking.entity.Payment;
import com.vivu.booking.payment.gateway.PaymentGateway;
import com.vivu.booking.utils.AppProperties;
import com.vivu.booking.utils.JsonUtils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Cong gia lap tren 103.216.117.40:8082 (Fastify, route {@code /v1/payments}).
 * Khong can them dependency HTTP client — dung HttpURLConnection san co (JDK 8+).
 *
 * Tom tat flow tu probe 2026-09-08:
 *  POST /v1/payments {merchant_id, amount, currency, payment_method=card, return_url, webhook_url, merchant_reference?, scenario?}
 *   -> 201 {payment_id (pay_*), checkout_url (http://103.216.117.40:5173/checkout/{id}), status=created}
 *  GET  /v1/payments/{id}
 *  POST /v1/payments/{id}/confirm {payment_method=card, card:{number, expiry, cvv}}
 *    - 4242...4242 -> succeeded, 4000...0002 -> failed, Webhook: POST webhook_url {event: payment.succeeded|failed}
 */
public class FakeBankGateway implements PaymentGateway {

    public static final String NAME = "FAKEBANK";

    private final String baseUrl;     // http://fake-bank-api:8080 (compose internal) hoac http://103.216.117.40:8082
    private final String merchantId;
    private final String webhookBase; // /api/payments/webhook* (da cau hinh webhook_url)

    public FakeBankGateway() {
        this(
                AppProperties.get("payment.fakebank.base-url", "http://103.216.117.40:8082"),
                AppProperties.get("payment.fakebank.merchant-id", "vivu-booking"),
                AppProperties.get("payment.webhook-base", "")
        );
    }
    public FakeBankGateway(String baseUrl, String merchantId, String webhookBase) {
        this.baseUrl = baseUrl == null ? "http://103.216.117.40:8082" : baseUrl.replaceAll("/$", "");
        this.merchantId = merchantId == null || merchantId.isBlank() ? "vivu-booking" : merchantId;
        this.webhookBase = webhookBase == null ? "" : webhookBase;
    }

    @Override public String name() { return NAME; }

    @Override
    public GatewayRef createRef(Payment payment, Booking booking, String clientIp, String returnUrl) {
        String webhookUrl = buildWebhookUrl(returnUrl);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("merchant_id", merchantId);
        body.put("amount", payment.getAmount().longValue()); // fake-bank spec expects amount in VND
        body.put("currency", "VND");
        body.put("payment_method", "card");
        body.put("return_url", returnUrl);
        body.put("webhook_url", webhookUrl);
        body.put("merchant_reference", booking.getBookingCode());
        // scenario: card_success de test tp 4242 khi confirm
        body.put("scenario", AppProperties.get("payment.fakebank.scenario", "card_success"));
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("booking_id", booking.getId());
        body.put("metadata", meta);

        String json = JsonUtils.toJson(body);
        String resp = post(baseUrl + "/v1/payments", json);
        JsonNode node = JsonUtils.fromJson(resp, JsonNode.class);
        String payId = node.path("payment_id").asText(null);
        String checkout = node.path("checkout_url").asText(null);
        if (payId == null || payId.isBlank()) {
            throw new RuntimeException("Fake-bank create payment khong tra payment_id: " + resp);
        }
        if (checkout == null || checkout.isBlank()) checkout = baseUrl + "/checkout/" + payId;
        return new GatewayRef(payId, checkout);
    }

    @Override
    public java.util.Optional<PaymentGateway.GatewayStatus> queryStatus(String gatewayRef) {
        // Fake-bank khong co giao thuc truy van "lay trang thai 1 payment" rieng — nhung co webhook.
        // Ta dung GET /v1/payments/{id} (co trong probe 2026-09-08) de xac minh lai so tien + status
        // thay vi tin webhook tuyen bo. Neu lab chua expose GET, viec goi se 404 — caller coi nhu
        // "cong khong ho tro re-query", chuyen sang xac minh bang chu ky (VnPay) hoac doi caller
        // lam theo kenh khac.
        String body = get(baseUrl + "/v1/payments/" + java.net.URLEncoder.encode(gatewayRef, java.nio.charset.StandardCharsets.UTF_8));
        JsonNode n = JsonUtils.fromJson(body, JsonNode.class);
        String rawStatus = n.path("status").asText(n.path("payment_status").asText(""));
        if (rawStatus == null || rawStatus.isBlank()) rawStatus = n.path("state").asText("");
        // Chuan hoa: succeeded/failed/pending — PaymentGateway.GatewayStatus tu nhan dien tu khoa.
        if (rawStatus == null || rawStatus.isBlank()) rawStatus = "pending";
        java.math.BigDecimal amt = null;
        JsonNode an = n.get("amount");
        if (an != null && (an.isNumber() || an.isTextual())) {
            try { amt = new java.math.BigDecimal(an.asText()); } catch (Exception ignore) {}
        }
        String cur = n.path("currency").asText("VND");
        return java.util.Optional.of(new PaymentGateway.GatewayStatus(rawStatus, amt, cur));
    }

    private String buildWebhookUrl(String returnUrl) {
        if (webhookBase != null && !webhookBase.isBlank()) return webhookBase;
        // fallback: BE publicOrigin + /api/payments/webhook (so sanh tu returnUrl)
        try {
            URL ru = new URL(returnUrl);
            String origin = ru.getProtocol() + "://" + ru.getHost() + (ru.getPort() != -1 ? ":" + ru.getPort() : "");
            return origin.replace(":3001", ":8081") + "/api/payments/webhook";
        } catch (Exception e) {
            return "http://103.216.117.40:8081/api/payments/webhook";
        }
    }

    // ---- HTTP helpers ----

    private static String get(String url) {
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(url).openConnection();
            c.setRequestMethod("GET");
            c.setRequestProperty("Accept", "application/json");
            c.setConnectTimeout(4000);
            c.setReadTimeout(6000);
            int code = c.getResponseCode();
            InputStream is = (code >= 200 && code < 300) ? c.getInputStream() : c.getErrorStream();
            String body = is == null ? "" : new String(is.readAllBytes(), StandardCharsets.UTF_8);
            if (code < 200 || code >= 300) throw new RuntimeException("Fake-bank GET " + code + ": " + body);
            return body;
        } catch (RuntimeException re) { throw re; }
        catch (Exception e) { throw new RuntimeException("Goi fake-bank GET that bai: " + e.getMessage(), e); }
        finally { if (c != null) c.disconnect(); }
    }

    private static String post(String url, String json) {
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(url).openConnection();
            c.setRequestMethod("POST");
            c.setRequestProperty("Content-Type", "application/json");
            c.setConnectTimeout(5000);
            c.setReadTimeout(8000);
            c.setDoOutput(true);
            try (OutputStream os = c.getOutputStream()) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
            }
            int code = c.getResponseCode();
            InputStream is = (code >= 200 && code < 300) ? c.getInputStream() : c.getErrorStream();
            String body = is == null ? "" : new String(is.readAllBytes(), StandardCharsets.UTF_8);
            if (code < 200 || code >= 300) throw new RuntimeException("Fake-bank HTTP " + code + ": " + body);
            return body;
        } catch (RuntimeException re) { throw re; }
        catch (Exception e) { throw new RuntimeException("Goi fake-bank that bai: " + e.getMessage(), e); }
        finally { if (c != null) c.disconnect(); }
    }
}
