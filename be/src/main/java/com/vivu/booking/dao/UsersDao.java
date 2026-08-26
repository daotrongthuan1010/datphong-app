package com.vivu.booking.dao;

import com.vivu.booking.dto.response.UsersLoginResponse;
import com.vivu.booking.dto.response.UsersResponse;
import com.vivu.booking.entity.Room;
import com.vivu.booking.entity.User;
import com.vivu.booking.enums.RoomStatus;
import com.vivu.booking.enums.RoomType;
import com.vivu.booking.enums.UserStatus;
import com.vivu.booking.enums.UserType;

import java.util.List;
import java.util.Optional;

public class UsersDao extends BaseDao<User, Long> {
    public UsersDao() {
        super(User.class);
    }

    public UsersLoginResponse getRolebyUsername(String username) {
        String sql = """      
                    SELECT u.fullName,
                        u.username,
                       u.password,
                       r.code
                FROM users u
                JOIN roles r ON u.role_id = r.id
                WHERE u.username = :username
                """;
        return read(session ->
                session.createNativeQuery(sql, Object[].class)
                        .setParameter("username", username)
                        .getResultStream()
                        .map(row -> new UsersLoginResponse(
                                (String) row[0],
                                (String) row[1],
                                (String) row[2],
                                (String) row[3]
                        ))
                        .findFirst()
                        .orElse(null)
        );
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
            StringBuilder hql = new StringBuilder("select u from User u join fetch u.role r where u.active = true");
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
            return q.getResultList();
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
}

