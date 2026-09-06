package com.vivu.booking.payment.gateway.vnpay;

import com.vivu.booking.entity.Booking;
import com.vivu.booking.entity.Payment;
import com.vivu.booking.payment.gateway.PaymentGateway;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class VnPayGateway implements PaymentGateway {
    private static final DateTimeFormatter FORMATTER  = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Override
    public String createPaymentUrl(Payment payment, Booking booking, String clientIp, String returnUrl) {
            Map<String, String> params = new HashMap<>();
            params.put("vnp_Version",VnPayConfig.VERSION);
            params.put("vnp_Command",VnPayConfig.COMMAND);
            params.put("vnp_TmnCode",VnPayConfig.TMM_CODE);
            BigDecimal vnpAmount = payment.getAmount().multiply(BigDecimal.valueOf(100));
            params.put("vnp_Amount",vnpAmount.toBigIntegerExact().toString());
            params.put("vnp_CurrCode","VND");
            params.put("vnp_TxnRef",payment.getId().toString());
            params.put("vnp_OrderInfo","Thanh toan booking"+booking.getBookingCode());
            params.put("vnp_OrderType", "other");
            params.put("vnp_Locale", "vn");
            params.put("vnp_ReturnUrl",returnUrl);
            params.put("vnp_IpAddr",clientIp);
            LocalDateTime now = LocalDateTime.now();
            params.put("vnp_CreateDate",now.format(FORMATTER));
            params.put("vnp_ExpireDate",booking.getHoldExpiresAt().format(FORMATTER));
            return VnPayUtil.buildPaymentUrl(params);

    }
}
