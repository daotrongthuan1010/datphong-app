package com.vivu.booking.dao;

import com.vivu.booking.entity.Room;
import com.vivu.booking.enums.RoomStatus;
import com.vivu.booking.enums.RoomType;

import java.util.List;
import java.util.Optional;

public class RoomDao extends BaseDao<Room, Long> {

    public RoomDao() {
        super(Room.class);
    }

    public Optional<Room> findByCode(String code) {
        return read(s -> s.createQuery("from Room where code = :code", Room.class)
                .setParameter("code", code).uniqueResultOptional());
    }

    public boolean existsByCode(String code) {
        return read(s -> s.createQuery("select count(r) from Room r where code=:code", Long.class)
                .setParameter("code", code).getSingleResult() > 0);
    }

    public List<Room> search(RoomType type, RoomStatus status, String keyword, int page, int size) {
        return read(s -> {
            StringBuilder hql = new StringBuilder("from Room r where r.active = true");
            if (type != null) hql.append(" and r.type = :type");
            if (status != null) hql.append(" and r.status = :status");
            if (keyword != null && !keyword.isBlank())
                hql.append(" and (lower(r.name) like :kw or lower(r.code) like :kw)");
            hql.append(" order by r.id desc");
            var q = s.createQuery(hql.toString(), Room.class);
            if (type != null) q.setParameter("type", type);
            if (status != null) q.setParameter("status", status);
            if (keyword != null && !keyword.isBlank()) q.setParameter("kw", "%" + keyword.toLowerCase() + "%");
            q.setFirstResult(page * size);
            q.setMaxResults(size);
            return q.getResultList();
        });
    }

    public long countSearch(RoomType type, RoomStatus status, String keyword) {
        return read(s -> {
            StringBuilder hql = new StringBuilder("select count(r) from Room r where r.active = true");
            if (type != null) hql.append(" and r.type = :type");
            if (status != null) hql.append(" and r.status = :status");
            if (keyword != null && !keyword.isBlank())
                hql.append(" and (lower(r.name) like :kw or lower(r.code) like :kw)");
            var q = s.createQuery(hql.toString(), Long.class);
            if (type != null) q.setParameter("type", type);
            if (status != null) q.setParameter("status", status);
            if (keyword != null && !keyword.isBlank()) q.setParameter("kw", "%" + keyword.toLowerCase() + "%");
            return q.getSingleResult();
        });
    }
}
