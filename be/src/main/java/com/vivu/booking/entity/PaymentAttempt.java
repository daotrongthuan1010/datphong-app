package com.vivu.booking.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Một <b>lần phát hành mã cổng</b> cho một payment — bảng chống mất tiền khi khách bấm "Thanh toán" nhiều lần.
 *
 * <h2>Vấn đề nó giải quyết</h2>
 * <pre>
 * Khách bấm "Thanh toán" 2 lần (double-click / retry):
 *   Lần 1: Payment(PENDING).gatewayTransactionRef = pay_aaa — FE mở tab checkout/pay_aaa
 *   Lần 2: gán đè thành pay_bbb — tab pay_aaa vẫn mở
 *   Khách trả tiền ở tab pay_aaa → webhook gửi pay_aaa → BE tìm không ra → tiền đã trừ, booking không xác nhận
 * </pre>
 * Bảng này lưu <b>mọi mã từng phát hành</b> nên webhook với mã nào cũng tra ngược được về payment.
 *
 * <p>Mọi payment chỉ có 1 dòng trong {@link Payment} (nguồn chân lý về tiền), nhưng có thể có
 * nhiều PaymentAttempt — mỗi dòng tương ứng một {@code gateway_transaction_ref} khác nhau.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "payment_attempts", uniqueConstraints = {
        @UniqueConstraint(name = "uk_payment_attempts_ref", columnNames = "gatewayTransactionRef")
})
public class PaymentAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    /** Mã cổng phát hành lần này (fake-bank {@code pay_*}, VNPay {@code vnp_TxnRef}). */
    @Column(nullable = false, length = 100, unique = true)
    private String gatewayTransactionRef;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 5)
    @Builder.Default
    private String currency = "VND";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
