package com.vivu.booking.entity;

import com.vivu.booking.enums.MediaTypeEnum;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "room_images")
public class RoomImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(nullable = false, length = 255)
    private String url;

    /** IMAGE hoặc VIDEO — cùng bảng room_images, phân biệt bằng cột này. */
    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false, length = 10)
    @Builder.Default
    private MediaTypeEnum mediaType = MediaTypeEnum.IMAGE;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;
}
