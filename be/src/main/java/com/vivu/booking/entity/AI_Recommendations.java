package com.vivu.booking.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name= "ai_recommendations")
public class AI_Recommendations {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @Column(name = "score", nullable = false, columnDefinition = "DECIMAL(6,1) UNSIGNED")
    private Double score;

    @Column(name = "model_version", nullable = false)
    private String model_version;

    @CreationTimestamp
    @Column(name = "generated_at", updatable = false)
    private LocalDateTime generatedAt;
}
