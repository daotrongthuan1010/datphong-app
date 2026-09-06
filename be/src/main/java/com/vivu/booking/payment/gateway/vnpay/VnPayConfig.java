package com.vivu.booking.payment.gateway.vnpay;

public final class VnPayConfig {
    private VnPayConfig() {}
    public static final String PAY_URL="https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
    public static final String VERSION="2.1.0";
    public static final String COMMAND="pay";
    public static final String TMM_CODE=System.getenv("VNPAY_TMN_CODE");
    public static final String HASH_SECRET=System.getenv("VNPAY_HASH_SECRET");
}
