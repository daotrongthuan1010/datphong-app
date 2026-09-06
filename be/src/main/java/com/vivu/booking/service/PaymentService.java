package com.vivu.booking.service;

import com.vivu.booking.dto.request.PaymentRequest;
import com.vivu.booking.dto.response.PaymentResponse;

public interface PaymentService {
    PaymentResponse createPayment(PaymentRequest req,Long userId,String clientId);
}
