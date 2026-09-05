package com.vivu.booking.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Dùng cho các endpoint webhook callback từ cổng thanh toán
 * (POST /api/payments/vnpay/callback, /momo/callback, /zalopay/callback).
 * Mỗi cổng có bộ tham số riêng (VD: VNPay dùng tiền tố vnp_*) nên giữ thêm
 * rawParams để Service tự verify chữ ký (signature) đúng theo tài liệu từng cổng,
 * đồng thời expose sẵn vài field phổ biến để code Service gọn hơn.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentCallbackRequest {

    @NotBlank(message = "Mã giao dịch cổng thanh toán không được để trống")
    private String gatewayTransactionRef;

    private BigDecimal amount;

    /** Mã kết quả do cổng trả về (VD: "00" = thành công với VNPay) */
    private String resultCode;

    private String message;

    /** Chữ ký cổng gửi kèm để verify request có đúng từ cổng không, chống giả mạo. */
    private String signature;

    /** Toàn bộ tham số gốc cổng gửi lên, dùng để build lại chuỗi ký khi verify signature. */
    private Map<String, String> rawParams;
}
