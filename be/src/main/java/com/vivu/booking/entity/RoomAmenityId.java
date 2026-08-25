package com.vivu.booking.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class RoomAmenityId implements Serializable {

    @Column(name = "room_id")
    private Long roomId;

    @Column(name = "amenity_id")
    private Long amenityId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RoomAmenityId that)) return false;
        return Objects.equals(roomId, that.roomId) && Objects.equals(amenityId, that.amenityId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(roomId, amenityId);
    }
}
