package com.vivu.booking.dto.response;

import com.vivu.booking.enums.RoomStatus;
import com.vivu.booking.enums.RoomType;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomResponse {
    private Long id;
    private String code;
    private String name;
    private RoomType type;
    private RoomStatus status;
    private Integer capacity;
    private Long pricePerNight;
    private String description;
    private String imageUrl;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
