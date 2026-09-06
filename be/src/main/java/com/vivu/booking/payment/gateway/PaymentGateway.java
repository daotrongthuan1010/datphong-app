package com.vivu.booking.payment.gateway;

import com.vivu.booking.entity.Booking;
import com.vivu.booking.entity.Payment;

public interface PaymentGateway {
    String createPaymentUrl(Payment payment, Booking booking,String clientIp,String returnUrl);
}
