package com.vivu.booking.dao;

import com.vivu.booking.entity.RoomImage;
import jakarta.persistence.EntityManager;
import java.util.List;

public class RoomImageDao extends BaseDao<RoomImage, Long> {

    public RoomImageDao() {
        super(RoomImage.class);
    }

    public List<RoomImage> findByRoomIdOrdered(Long roomId, EntityManager em) {
        return em.createQuery(
                        "SELECT i FROM RoomImage i WHERE i.room.id = :roomId ORDER BY i.sortOrder ASC",
                        RoomImage.class)
                .setParameter("roomId", roomId)
                .getResultList();
    }
}
