package com.vivu.booking.entity;

import com.vivu.booking.enums.BehaviorActionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "user_behavior_logs")
public class UserBehaviorLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private Room room;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "action_type", nullable = false, columnDefinition = "behavior_action_type")
    private BehaviorActionType actionType;

    // Lưu tiêu chí tìm kiếm: vị trí, ngày check-in/out, số khách, bộ lọc...
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "search_query", columnDefinition = "jsonb")
    private Map<String, Object> searchQuery;

    @Column(name = "dwell_time_seconds")
    private Integer dwellTimeSeconds;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
