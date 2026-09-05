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
        return search(type, status, keyword, null, null, null, page, size, null, null);
    }

    public long countSearch(RoomType type, RoomStatus status, String keyword) {
        return countSearch(type, status, keyword, null, null, null);
    }

    /**
     * Tim kiem nang cao — Home: loc them gia, suc chua + sap xep.
     * sortBy: "pricePerNight" | "createdAt" | "name" (default: id desc).
     */
    public List<Room> search(RoomType type, RoomStatus status, String keyword,
                             Long minPrice, Long maxPrice, Integer minCapacity,
                             int page, int size, String sortBy, String sortDir) {
        return read(s -> {
            StringBuilder hql = new StringBuilder("from Room r where r.active = true");
            if (type != null) hql.append(" and r.type = :type");
            if (status != null) hql.append(" and r.status = :status");
            if (keyword != null && !keyword.isBlank())
                hql.append(" and (lower(r.name) like :kw or lower(r.code) like :kw)");
            if (minPrice != null) hql.append(" and r.pricePerNight >= :minPrice");
            if (maxPrice != null) hql.append(" and r.pricePerNight <= :maxPrice");
            if (minCapacity != null) hql.append(" and r.capacity >= :minCapacity");
            boolean desc = !"asc".equalsIgnoreCase(sortDir);
            String col = switch (sortBy == null ? "" : sortBy) {
                case "pricePerNight" -> "r.pricePerNight";
                case "name" -> "r.name";
                case "createdAt" -> "r.createdAt";
                default -> "r.id";
            };
            hql.append(" order by ").append(col).append(desc ? " desc" : " asc");
            var q = s.createQuery(hql.toString(), Room.class);
            if (type != null) q.setParameter("type", type);
            if (status != null) q.setParameter("status", status);
            if (keyword != null && !keyword.isBlank()) q.setParameter("kw", "%" + keyword.toLowerCase() + "%");
            if (minPrice != null) q.setParameter("minPrice", minPrice);
            if (maxPrice != null) q.setParameter("maxPrice", maxPrice);
            if (minCapacity != null) q.setParameter("minCapacity", minCapacity);
            q.setFirstResult(page * size);
            q.setMaxResults(size);
            return q.getResultList();
        });
    }

    public long countSearch(RoomType type, RoomStatus status, String keyword,
                            Long minPrice, Long maxPrice, Integer minCapacity) {
        return read(s -> {
            StringBuilder hql = new StringBuilder("select count(r) from Room r where r.active = true");
            if (type != null) hql.append(" and r.type = :type");
            if (status != null) hql.append(" and r.status = :status");
            if (keyword != null && !keyword.isBlank())
                hql.append(" and (lower(r.name) like :kw or lower(r.code) like :kw)");
            if (minPrice != null) hql.append(" and r.pricePerNight >= :minPrice");
            if (maxPrice != null) hql.append(" and r.pricePerNight <= :maxPrice");
            if (minCapacity != null) hql.append(" and r.capacity >= :minCapacity");
            var q = s.createQuery(hql.toString(), Long.class);
            if (type != null) q.setParameter("type", type);
            if (status != null) q.setParameter("status", status);
            if (keyword != null && !keyword.isBlank()) q.setParameter("kw", "%" + keyword.toLowerCase() + "%");
            if (minPrice != null) q.setParameter("minPrice", minPrice);
            if (maxPrice != null) q.setParameter("maxPrice", maxPrice);
            if (minCapacity != null) q.setParameter("minCapacity", minCapacity);
            return q.getSingleResult();
        });
    }
}
