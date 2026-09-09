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

    @Size(max = 300)
    private String address;

    /** Thay toàn bộ tiện nghi của phòng bằng danh sách này (null = không đổi). */
    private java.util.List<Long> amenityIds;

    private Boolean active;
}
