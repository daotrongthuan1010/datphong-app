package com.vivu.booking.dao;

import com.vivu.booking.entity.CollectionItem;
import jakarta.persistence.EntityManager;
import java.util.List;

public class CollectionItemDao extends BaseDao<CollectionItem, Long> {

    public CollectionItemDao() {
        super(CollectionItem.class);
    }

    /** UNIQUE(collection_id, room_id) leftmost - danh sách phòng trong 1 bộ sưu tập */
    public List<CollectionItem> findByCollectionId(Long collectionId, EntityManager em) {
        return em.createQuery(
                        "SELECT i FROM CollectionItem i WHERE i.collection.id = :collectionId " +
                                "ORDER BY i.addedAt DESC", CollectionItem.class)
                .setParameter("collectionId", collectionId)
                .getResultList();
    }

    /** idx_collection_items_room_id - "phòng này nằm trong những bộ sưu tập nào" */
    public List<CollectionItem> findByRoomId(Long roomId, EntityManager em) {
        return em.createQuery(
                        "SELECT i FROM CollectionItem i WHERE i.room.id = :roomId", CollectionItem.class)
                .setParameter("roomId", roomId)
                .getResultList();
    }

    /** Kiểm tra tồn tại trước khi thêm, tránh vi phạm UNIQUE constraint. */
    public boolean existsInCollection(Long collectionId, Long roomId, EntityManager em) {
        Long count = em.createQuery(
                        "SELECT COUNT(i) FROM CollectionItem i WHERE i.collection.id = :collectionId " +
                                "AND i.room.id = :roomId", Long.class)
                .setParameter("collectionId", collectionId)
                .setParameter("roomId", roomId)
                .getSingleResult();
        return count > 0;
    }
}
