package com.vivu.booking.payment.gateway.vnpay;

import com.vivu.booking.entity.Booking;
import com.vivu.booking.entity.Payment;
import com.vivu.booking.payment.gateway.PaymentGateway;
import com.vivu.booking.utils.AppProperties;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class VnPayGateway implements PaymentGateway {

    public static final String NAME = "VNPAY";
    // 30 phu tu bat dau build url de khach du thoi gian lam tien (truoc day dung holdExpiresAt lao sau ~ vai phut ma system timeout 5p xunng dot)
    private static final int EXPIRE_MINUTES = Integer.parseInt(AppProperties.get("payment.vnpay.expire-minutes", "30"));
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Override public String name() { return NAME; }

    @Override
    public GatewayRef createRef(Payment payment, Booking booking, String clientIp, String returnUrl) {
        Map<String, String> p = new HashMap<>();
        p.put("vnp_Version", VnPayConfig.VERSION);
        p.put("vnp_Command", VnPayConfig.COMMAND);
        p.put("vnp_TmnCode", VnPayConfig.TMM_CODE);
        BigDecimal vnpAmount = payment.getAmount().multiply(BigDecimal.valueOf(100));
        p.put("vnp_Amount", vnpAmount.toBigIntegerExact().toString());
        p.put("vnp_CurrCode", "VND");
        p.put("vnp_TxnRef", payment.getId().toString());
        p.put("vnp_OrderInfo", "Thanh toan booking " + booking.getBookingCode());
        p.put("vnp_OrderType", "other");
        p.put("vnp_Locale", "vn");
        p.put("vnp_ReturnUrl", returnUrl);
        p.put("vnp_IpAddr", clientIp);
        LocalDateTime now = LocalDateTime.now();
        p.put("vnp_CreateDate", now.format(FMT));
        p.put("vnp_ExpireDate", now.plusMinutes(EXPIRE_MINUTES).format(FMT));
        String url = VnPayUtil.buildPaymentUrl(p);
        return new GatewayRef(payment.getId().toString(), url);
    }

    @Override
    public java.util.Optional<GatewayStatus> queryStatus(String gatewayRef) {
        // VNPay khong co API hoi trang thai — tinh bao mat phai dua vao chu ky tr == IPN verifier
        // o VnPayReturnServlet (build lai hash tu params). Caller thay empty se doi chu ky dung
        // roi moi cho qua; khong co chu ky dung la reject.
        return java.util.Optional.empty();
    }

    /** Back-compat cho code cu goi ra String. */
    public String createPaymentUrl(Payment payment, Booking booking, String clientIp, String returnUrl) {
        return createRef(payment, booking, clientIp, returnUrl).checkoutUrl();
    }
}
