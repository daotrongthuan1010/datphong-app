package com.vivu.booking.payment.gateway;

import com.vivu.booking.utils.AppProperties;

/** Factory chon cong theo config {@code payment.gateway=fakebank|vnpay}. */
public final class PaymentGatewayFactory {
    private PaymentGatewayFactory() {}

    public static PaymentGateway resolve() {
        String which = AppProperties.get("payment.gateway", "fakebank").trim().toLowerCase();
        if ("vnpay".equals(which) || "vn_pay".equals(which)) {
            return new com.vivu.booking.payment.gateway.vnpay.VnPayGateway();
        }
        return new com.vivu.booking.payment.gateway.fakebank.FakeBankGateway();
    }
}
