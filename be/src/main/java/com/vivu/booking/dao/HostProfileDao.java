package com.vivu.booking.dao;

import com.vivu.booking.entity.HostProfile;
import com.vivu.booking.enums.HostStatus;

import java.util.List;
import java.util.Optional;

public class HostProfileDao extends BaseDao<HostProfile, Long> {

    public HostProfileDao() {
        super(HostProfile.class);
    }

    /**
     * Tìm HostProfile theo username
     */
    public Optional<HostProfile> findByUsername(String username) {

        return read(h ->
                h.createQuery("""
                        select hp
                        from HostProfile hp
                        join fetch hp.user u
                        where u.username = :username
                        """, HostProfile.class)
                        .setParameter("username", username)
                        .uniqueResultOptional()
        );
    }

    /**
     * Kiểm tra user đã có HostProfile hay chưa
     */
    public boolean existsByUserId(Long userId) {

        return read(h ->
                h.createQuery("""
                        select count(hp)
                        from HostProfile hp
                        where hp.user.id = :userId
                        """, Long.class)
                        .setParameter("userId", userId)
                        .getSingleResult() > 0
        );
    }

    /**
     * Tìm kiếm + phân trang HostProfile
     */
    public List<HostProfile> search(
            HostStatus status,
            String keyword,
            int page,
            int size
    ) {

        return read(h -> {

            StringBuilder hql = new StringBuilder("""
                    select distinct hp
                    from HostProfile hp
                    join fetch hp.user u
                    where 1 = 1
                    """);

            if (status != null) {
                hql.append("""
                        and hp.hostStatus = :status
                        """);
            }

            if (keyword != null && !keyword.isBlank()) {
                hql.append("""
                        and (
                            lower(hp.displayName) like :kw
                            or lower(hp.businessName) like :kw
                            or lower(u.username) like :kw
                        )
                        """);
            }

            hql.append(" order by hp.id desc");

            var query =
                    h.createQuery(
                            hql.toString(),
                            HostProfile.class
                    );

            if (status != null) {
                query.setParameter("status", status);
            }

            if (keyword != null && !keyword.isBlank()) {
                query.setParameter(
                        "kw",
                        "%" + keyword.trim().toLowerCase() + "%"
                );
            }

            query.setFirstResult(page * size);
            query.setMaxResults(size);

            return query.getResultList();
        });
    }

    /**
     * Đếm số HostProfile theo điều kiện tìm kiếm
     */
    public long countSearch(
            HostStatus status,
            String keyword
    ) {

        return read(h -> {

            StringBuilder hql = new StringBuilder("""
                    select count(hp)
                    from HostProfile hp
                    join hp.user u
                    where 1 = 1
                    """);

            if (status != null) {
                hql.append("""
                        and hp.hostStatus = :status
                        """);
            }

            if (keyword != null && !keyword.isBlank()) {
                hql.append("""
                        and (
                            lower(hp.displayName) like :kw
                            or lower(hp.businessName) like :kw
                            or lower(u.username) like :kw
                        )
                        """);
            }

            var query =
                    h.createQuery(
                            hql.toString(),
                            Long.class
                    );

            if (status != null) {
                query.setParameter(
                        "status",
                        status
                );
            }

            if (keyword != null && !keyword.isBlank()) {
                query.setParameter(
                        "kw",
                        "%" + keyword.trim().toLowerCase() + "%"
                );
            }

            return query.getSingleResult();
        });
    }
    public Optional<HostProfile> findByIdWithUser(Long id) {

        return read(session ->
                session.createQuery("""
                    SELECT DISTINCT hp
                    FROM HostProfile hp
                    JOIN FETCH hp.user u
                    LEFT JOIN FETCH u.role
                    WHERE hp.id = :id
                    """, HostProfile.class)
                        .setParameter("id", id)
                        .uniqueResultOptional()
        );
    }
}