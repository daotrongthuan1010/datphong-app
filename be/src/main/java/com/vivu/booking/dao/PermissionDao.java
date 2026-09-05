package com.vivu.booking.dao;

import com.vivu.booking.entity.Permission;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;

public class PermissionDao extends BaseDao<Permission, Long> {

    public PermissionDao() {
        super(Permission.class);
    }

    /** UNIQUE index sẵn có trên permissions.code */
    public Optional<Permission> findByCode(String code, EntityManager em) {
        List<Permission> result = em.createQuery(
                        "SELECT p FROM Permission p WHERE p.code = :code", Permission.class)
                .setParameter("code", code)
                .getResultList();
        return result.stream().findFirst();
    }
}
