package com.vivu.booking.dao;

import com.vivu.booking.dto.response.UsersLoginResponse;

import com.vivu.booking.entity.User;
import com.vivu.booking.enums.UserStatus;
import com.vivu.booking.enums.UserType;
import org.hibernate.Hibernate;
import org.hibernate.Session;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class UsersDao extends BaseDao<User, Long> {
    public UsersDao() {
        super(User.class);
    }

    public UsersLoginResponse getRolebyUsername(String username) {
        String sql = """
        SELECT
            u.fullname,
            u.username,
            u.password,
            STRING_AGG(DISTINCT r.code, ',') AS roles
        FROM users u
        JOIN user_roles ur
            ON u.id = ur.user_id
        JOIN roles r
            ON ur.role_id = r.id
        WHERE u.username = :username
        GROUP BY
            u.id,
            u.fullname,
            u.username,
            u.password
        """;
//chuyền set vào để có nhiều role
        return read(session ->
                session.createNativeQuery(sql, Object[].class)
                        .setParameter("username", username)
                        .getResultStream()
                        .map(row -> new UsersLoginResponse(
                                (String) row[0],
                                (String) row[1],
                                (String) row[2],
                                row[3] != null
                                        ? Set.of(((String) row[3]).split(","))
                                        : new HashSet<>()
                        ))
                        .findFirst()
                        .orElse(null)
        );
    }

    public Optional<User> findByUsername(String username) {
        return read(s -> s.createQuery("from User where username = :username", User.class)
                .setParameter("username", username).uniqueResultOptional());
    }

    /** Dùng cho login/refresh-token: load kèm role để tránh LazyInitializationException sau khi Session đóng. */
    public Optional<User> findByUsernameWithRoles(String username) {
        return read(session -> {
            User user = session.createQuery("""
                select distinct u
                from User u
                left join fetch u.role
                where u.username = :username
                """, User.class)
                    .setParameter("username", username)
                    .uniqueResult();
            return Optional.ofNullable(user);
        });
    }

    public boolean existsByUsername(String username) {
        return read(s -> s.createQuery("select count(u) from User u where u.username=:username", Long.class)
                .setParameter("username", username).getSingleResult() > 0);
    }

    public boolean existsByPhone(String phone) {
        return read(s -> s.createQuery("select count(u) from User u where u.phone=:phone", Long.class)
                .setParameter("phone", phone).getSingleResult() > 0);
    }

    public Optional<User> findByCode(String email) {
        return read(s -> s.createQuery("from User where email = :email", User.class)
                .setParameter("email", email).uniqueResultOptional());
    }

    public boolean existsByCode(String email) {
        return read(s -> s.createQuery("select count(r) from User r where email=:email", Long.class)
                .setParameter("email", email).getSingleResult() > 0);
    }

    public List<User> search(UserType type, UserStatus status, String keyword, int page, int size) {
        return read(s -> {
            StringBuilder hql = new StringBuilder("""
                select distinct u
                from User u
                where u.active = true
                """);
            if (type != null) hql.append(" and u.type = :type");
            if (status != null) hql.append(" and u.status = :status");
            if (keyword != null && !keyword.isBlank()){
                hql.append("""
                    and (
                        lower(u.fullName) like :kw
                        or lower(u.email) like :kw
                        or lower(u.username) like :kw
                        or lower(u.phone) like :kw
                    )
                    """);
            }
            hql.append(" order by u.id desc");
            var q = s.createQuery(hql.toString(), User.class);
            if (type != null) {q.setParameter("type", type);}
            if (status != null) {q.setParameter("status", status);}
            if (keyword != null && !keyword.isBlank()) {q.setParameter("kw", "%" + keyword.toLowerCase() + "%");}
            q.setFirstResult(page * size);
            q.setMaxResults(size);
            List<User> users = q.getResultList();

            // QUAN TRỌNG
            users.forEach(user ->
                    Hibernate.initialize(user.getRole())
            );

            return users;
        });
    }

    public long countSearch(UserType type, UserStatus status, String keyword) {
        return read(s -> {
            StringBuilder hql = new StringBuilder(
                    "select count(u) from User u where u.active = true"
            );
            if (type != null) {hql.append(" and u.type = :type");}
            if (status != null) {hql.append(" and u.status = :status");}
            if (keyword != null && !keyword.isBlank()) {
                hql.append("""
                    and (
                        lower(u.fullName) like :kw
                        or lower(u.email) like :kw
                        or lower(u.username) like :kw
                        or lower(u.phone) like :kw
                    )
                    """);
            }
            var q = s.createQuery(hql.toString(), Long.class);
            if (type != null) {q.setParameter("type", type);}
            if (status != null) {q.setParameter("status", status);}
            if (keyword != null && !keyword.isBlank()) {
                q.setParameter("kw", "%" + keyword.toLowerCase() + "%");
            }
            return q.getSingleResult();
        });
    }
    public Optional<User> findByIdWithRoles(Long id) {
        return read(session -> {
            User user = session.createQuery("""
                select distinct u
                from User u
                left join fetch u.role
                where u.id = :id
                """, User.class)
                    .setParameter("id", id)
                    .uniqueResult();

            return Optional.ofNullable(user);
        });
    }

}