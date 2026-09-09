package com.vivu.booking.dto.response;

import com.vivu.booking.entity.Amenity;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AmenityResponse {
    private Long id;
    private String name;
    private String icon;

    public static AmenityResponse from(Amenity e) {
        return AmenityResponse.builder()
                .id(e.getId())
                .name(e.getName())
                .icon(e.getIcon())
                .build();
    }
}
