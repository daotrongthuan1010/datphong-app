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
    /** Ly do that bai/tam dung (khong bao gio null khi status = FAILED). */
    private String failureReason;
    /** Ma tien — luon "VND" hien tai, giu de doi chieu webhook. */
    private String currency;
    //url để fe redirect sang vnpay/momo
    private String paymentUrl;
}
