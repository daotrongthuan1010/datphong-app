package com.vivu.booking.dao;

import com.vivu.booking.entity.RoomAmenity;
import com.vivu.booking.entity.RoomAmenityId;

import java.util.List;

public class RoomAmenityDao extends BaseDao<RoomAmenity, RoomAmenityId> {

    public RoomAmenityDao() {
        super(RoomAmenity.class);
    }

    public List<RoomAmenity> findByRoomId(Long roomId) {
        return read(s -> s.createQuery(
                        "from RoomAmenity ra join fetch ra.amenity where ra.room.id = :roomId", RoomAmenity.class)
                .setParameter("roomId", roomId)
                .getResultList());
    }

    public void deleteByRoomId(Long roomId) {
        tx(s -> {
            s.createMutationQuery("delete from RoomAmenity where room.id = :roomId")
                    .setParameter("roomId", roomId)
                    .executeUpdate();
            return null;
        });
    }
}
