package com.vivu.booking.dao;

import com.vivu.booking.entity.RoomImage;
import com.vivu.booking.enums.MediaTypeEnum;

import java.util.List;
import java.util.Optional;

public class RoomImageDao extends BaseDao<RoomImage, Long> {

    public RoomImageDao() {
        super(RoomImage.class);
    }

    public List<RoomImage> findByRoomIdOrdered(Long roomId) {
        return read(s -> s.createQuery("from RoomImage where room.id = :roomId order by sortOrder asc, id asc", RoomImage.class)
                .setParameter("roomId", roomId)
                .getResultList());
    }

    public List<RoomImage> findByRoomIdAndType(Long roomId, MediaTypeEnum type) {
        return read(s -> s.createQuery("from RoomImage where room.id = :roomId and mediaType = :type order by sortOrder asc, id asc", RoomImage.class)
                .setParameter("roomId", roomId)
                .setParameter("type", type)
                .getResultList());
    }

    public Optional<RoomImage> findByRoomIdAndId(Long roomId, Long mediaId) {
        return read(s -> s.createQuery("from RoomImage where room.id = :roomId and id = :id", RoomImage.class)
                .setParameter("roomId", roomId)
                .setParameter("id", mediaId)
                .uniqueResultOptional());
    }

    public void deleteByRoomId(Long roomId) {
        tx(s -> {
            s.createMutationQuery("delete from RoomImage where room.id = :roomId")
                    .setParameter("roomId", roomId)
                    .executeUpdate();
            return null;
        });
    }
}
