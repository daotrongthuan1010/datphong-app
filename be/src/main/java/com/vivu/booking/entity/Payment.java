package com.vivu.booking.entity;

import com.vivu.booking.enums.PaymentMethodType;
import com.vivu.booking.enums.PaymentStatusType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Mot giao dich thanh toan cua mot booking.
 *
 * <h2>Vì sao 1 booking chỉ có 1 Payment, nhưng có thể nhiều {@link PaymentAttempt}?</h2>
 * <p>
 * {@code @OneToOne} ở đây là <b>nguồn chân lý về tiền</b>: booking này đã thu bao nhiêu,
 * trạng thái cuối là gì. Còn mỗi lần khách bấm "Thanh toán" rồi bị đẩy sang cổng
 * (fake-bank/VNPay) là một <b>intent</b> riêng, có {@code gateway_transaction_ref} riêng.
 *
 * <p>Lỗi kinh điển nếu gộp hai khái niệm: khách bấm 2 lần → cổng cấp 2 mã {@code pay_*} →
 * nếu chỉ lưu 1 cột {@code gateway_transaction_ref} thì mã thứ nhất bị ghi đè. Khách lại
 * trả tiền ở tab mở từ mã thứ nhất → webhook gửi mã đó lên → BE không tìm ra payment →
 * <b>tiền đã trừ mà booking không được xác nhận</b>. {@link PaymentAttempt} sinh ra để
 * mọi mã cổng từng phát hành đều tra ngược được về payment.
 *
 * <h2>Vì sao khoá bi quan (SELECT ... FOR UPDATE) thay vì {@code @Version}?</h2>
 * <p>
 * {@link Wallet} dùng {@code @Version} vì số dư cộng/trừ liên tục và xung đột thì rollback
 * được. Payment thì khác: webhook có thể bắn lại nhiều lần, và ta cần <b>đọc trạng thái rồi
 * quyết định</b> trong cùng một transaction (đã SUCCESS thì bỏ qua, chưa thì xác nhận booking).
 * Khoá dòng {@code FOR UPDATE} giữ hai webhook song song chạy tuần tự, không bên nào đọc được
 * trạng thái cũ. Ngoài ra thêm {@code @Version} vào bảng đã có dữ liệu sẽ để {@code version=NULL}
 * cho mọi dòng cũ và làm hỏng lần update đầu tiên — không đáng đổi.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "payments", uniqueConstraints = {
        // 1 booking = 1 giao dịch tiền. Hibernate sinh unique cho @OneToOne, khai báo lại
        // ở đây để ý đồ rõ ràng (và để DB tạo mới có ràng buộc ngay cả khi không dùng hbm2ddl).
        @UniqueConstraint(name = "uk_payments_booking", columnNames = "booking_id")
})
public class Payment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "payment_method_type")
    private PaymentMethodType method;

    /**
     * Số tiền PHẢI thu — lấy từ {@code booking.totalPrice} do BE tự tính, không bao giờ
     * nhận từ request của khách. Webhook phải đối chiếu số tiền cổng báo về với cột này.
     */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 5)
    @Builder.Default
    private String currency = "VND";

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "payment_status_type")
    @Builder.Default
    private PaymentStatusType status = PaymentStatusType.PENDING;

    /**
     * Mã giao dịch của <b>lần intent gần nhất</b>. Giữ lại để tương thích các truy vấn cũ;
     * muốn tra cứu đầy đủ mọi mã từng phát hành thì dùng {@link PaymentAttempt}.
     */
    @Column(name = "gateway_transaction_ref", length = 100)
    private String gatewayTransactionRef;

    /** Lý do thất bại ghi lại từ cổng — FE hiển thị, và là bằng chứng khi đối soát/khiếu nại. */
    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;
}
