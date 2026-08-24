package com.vivu.booking.dto.request;

import com.vivu.booking.enums.RoomStatus;
import com.vivu.booking.enums.RoomType;
import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomCreateRequest {
    @NotBlank
    @Size(max = 20)
    private String code;

    @NotBlank
    @Size(max = 150)
    private String name;
    @NotNull
    private RoomType type;

    private RoomStatus status;

    @NotNull
    @Min(1)
    @Max(20)
    private Integer capacity;

    @NotNull
    @Min(0)
    private Long pricePerNight;

    @Size(max = 500)
    private String description;

    @Size(max = 500)
    private String imageUrl;
}
