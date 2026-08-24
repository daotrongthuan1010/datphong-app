package com.vivu.booking.mapper;

import com.vivu.booking.dto.request.RoomCreateRequest;
import com.vivu.booking.dto.response.RoomResponse;
import com.vivu.booking.entity.Room;
import com.vivu.booking.enums.RoomStatus;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RoomMapper {

    public static Room toEntity(RoomCreateRequest req) {
        return Room.builder()
                .code(req.getCode())
                .name(req.getName())
                .type(req.getType())
                .status(req.getStatus() != null ? req.getStatus() : RoomStatus.AVAILABLE)
                .capacity(req.getCapacity())
                .pricePerNight(req.getPricePerNight())
                .description(req.getDescription())
                .imageUrl(req.getImageUrl())
                .active(true)
                .build();
    }

    public static RoomResponse toResponse(Room e) {
        return RoomResponse.builder()
                .id(e.getId()).code(e.getCode()).name(e.getName())
                .type(e.getType()).status(e.getStatus())
                .capacity(e.getCapacity()).pricePerNight(e.getPricePerNight())
                .description(e.getDescription()).imageUrl(e.getImageUrl())
                .active(e.getActive())
                .createdAt(e.getCreatedAt()).updatedAt(e.getUpdatedAt())
                .build();
    }
}
