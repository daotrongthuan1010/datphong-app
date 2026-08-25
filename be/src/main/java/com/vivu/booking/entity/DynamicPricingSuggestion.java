package com.vivu.booking.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "dynamic_pricing_suggestions")
public class DynamicPricingSuggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate;

    @Column(name = "suggested_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal suggestedPrice;

    @Column(length = 255)
    private String reason;

    @Column(name = "model_version", length = 30)
    private String modelVersion;

    // Host đã áp dụng giá đề xuất này vào lịch phòng hay chưa
    @Column(nullable = false)
    @Builder.Default
    private Boolean applied = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
