package com.vivu.booking.mapper;

import com.vivu.booking.dto.request.PaymentRequest;
import com.vivu.booking.dto.response.PaymentResponse;
import com.vivu.booking.entity.Payment;
import com.vivu.booking.enums.PaymentStatusType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PaymentMapper {

    public static Payment toEntity(PaymentRequest req) {
        return Payment.builder()
                .method(req.getMethod())
                .status(PaymentStatusType.PENDING)
                .build();
    }

    public static PaymentResponse toResponse(Payment e) {
        if(e==null) {return null;}
        return PaymentResponse.builder()
                .id(e.getId())
                .bookingId(e.getBooking()!=null?e.getBooking().getId():null)
                .method(e.getMethod())
                .amount(e.getAmount())
                .status(e.getStatus())
                .gatewayTransactionRef(e.getGatewayTransactionRef())
                .paidAt(e.getPaidAt())
                .failureReason(e.getFailureReason())
                .currency(e.getCurrency())
                .paymentUrl(null)
                .build();
    }
}
