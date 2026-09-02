package com.vivu.booking.dao;

import com.vivu.booking.entity.Amenity;
import jakarta.persistence.EntityManager;
import java.util.Collection;
import java.util.List;

public class AmenityDao extends BaseDao<Amenity, Long> {

    public AmenityDao() {
        super(Amenity.class);
    }

    /** Bảng nhỏ, tra cứu nhiều id cùng lúc khi build filter tìm phòng theo tiện ích. */
    public List<Amenity> findByIds(Collection<Long> ids, EntityManager em) {
        return em.createQuery(
                        "SELECT a FROM Amenity a WHERE a.id IN :ids", Amenity.class)
                .setParameter("ids", ids)
                .getResultList();
    }
}
