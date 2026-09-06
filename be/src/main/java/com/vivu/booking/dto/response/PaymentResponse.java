package com.vivu.booking.dto.response;

import com.vivu.booking.entity.Booking;
import com.vivu.booking.enums.PaymentMethodType;
import com.vivu.booking.enums.PaymentStatusType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {
    private Long id;
    private Long bookingId;
    private PaymentMethodType method;
    private BigDecimal amount;
    private PaymentStatusType status;
    private String gatewayTransactionRef;
    private LocalDateTime paidAt;
    //url để fe redirect sang vnpay/momo
    private String paymentUrl;
}
