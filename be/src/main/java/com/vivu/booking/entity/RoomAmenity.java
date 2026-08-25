package com.vivu.booking.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "room_amenities")
public class RoomAmenity {

    @EmbeddedId
    private RoomAmenityId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("roomId")
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("amenityId")
    @JoinColumn(name = "amenity_id", nullable = false)
    private Amenity amenity;
}
