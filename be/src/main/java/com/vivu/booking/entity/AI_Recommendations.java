package com.vivu.booking.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * BẢN ĐÃ SỬA 3 lỗi của entity AI_Recommendations gốc - KHÔNG tự động ghi đè,
 * bạn tự so sánh rồi thay thế file gốc khi sẵn sàng (nhớ cập nhật lại
 * AiRecommendationMapper + AiRecommendationResponse cho khớp sau khi đổi).
 *
 * Đã sửa:
 * 1) Thêm lại field "id" riêng (Long, @Id @GeneratedValue) - bản gốc bị thiếu,
 *    @Id/@GeneratedValue vô tình gắn nhầm lên field "user".
 * 2) Đổi "role" (ManyToOne tới Role) thành "room" (ManyToOne tới Room) - đúng
 *    nghiệp vụ "gợi ý PHÒNG", không phải gợi ý role.
 * 3) Bỏ columnDefinition = "DECIMAL(6,1) UNSIGNED" (cú pháp MySQL, không hợp
 *    lệ với PostgreSQL) - dùng @Column(precision, scale) chuẩn JPA thay thế.
 * 4) Đổi field "model_version" (snake_case) thành "modelVersion" (chuẩn Java
 *    camelCase) - Lombok tự map sang cột "model_version" qua @Column(name=...).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "ai_recommendations")
public class AI_Recommendations {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(name = "score", nullable = false, precision = 5, scale = 4)
    private Double score;

    @Column(name = "model_version", length = 30)
    private String modelVersion;

    @CreationTimestamp
    @Column(name = "generated_at", updatable = false)
    private LocalDateTime generatedAt;
}
