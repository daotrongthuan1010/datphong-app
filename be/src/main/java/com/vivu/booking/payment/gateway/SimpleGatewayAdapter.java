package com.vivu.booking.payment.gateway;

import com.vivu.booking.entity.Booking;
import com.vivu.booking.entity.Payment;

/**
 * Adapter cho backwards compat: gateway cu chi tra URL chuoi.
 * Dung cho VNPay hien tai; FakeBankGateway tra ca ref.
 */
public final class SimpleGatewayAdapter implements PaymentGateway {

    private final PaymentGateway inner;

    public SimpleGatewayAdapter(PaymentGateway inner) { this.inner = inner; }

    @Override public String name() { return inner.name(); }

    @Override public GatewayRef createRef(Payment payment, Booking booking, String clientIp, String returnUrl) {
        return inner.createRef(payment, booking, clientIp, returnUrl);
    }

    /** Helper khi chi co URL (test/mock). */
    public static PaymentGateway of(String name, java.util.function.Function<String, String> urlFn) {
        return new PaymentGateway() {
            @Override public String name() { return name; }
            @Override public GatewayRef createRef(Payment p, Booking b, String ip, String ret) {
                return new GatewayRef(p.getId() == null ? "" : p.getId().toString(), urlFn.apply(ret));
            }
        };
    }
}
