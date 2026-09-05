package com.vivu.booking.entity;

import com.vivu.booking.enums.CalendarStatusType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "room_calendar", uniqueConstraints = @UniqueConstraint(columnNames = {"room_id", "calendar_date"}))
public class RoomCalendar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(name = "calendar_date", nullable = false)
    private LocalDate calendarDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private CalendarStatusType status = CalendarStatusType.AVAILABLE;

    @Column(name = "price_override", precision = 12, scale = 2)
    private BigDecimal priceOverride;
}
