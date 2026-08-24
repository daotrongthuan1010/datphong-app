package com.vivu.booking.dto.request;

import com.vivu.booking.enums.RoomStatus;
import com.vivu.booking.enums.RoomType;
import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomUpdateRequest {
    @Size(max = 150)
    private String name;

    private RoomType type;

    private RoomStatus status;

    @Min(1)
    @Max(20)
    private Integer capacity;

    @Min(0)
    private Long pricePerNight;

    @Size(max = 500)
    private String description;

    @Size(max = 500)
    private String imageUrl;

    private Boolean active;
}
