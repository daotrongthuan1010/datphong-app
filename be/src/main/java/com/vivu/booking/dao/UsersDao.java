package com.vivu.booking.dao;

import com.vivu.booking.dto.response.UsersLoginResponse;
import com.vivu.booking.dto.response.UsersResponse;
import com.vivu.booking.entity.User;
import com.vivu.booking.enums.UserStatus;

import java.util.List;

public class UsersDao extends BaseDao<User, Long> {
    public UsersDao() {
        super(User.class);
    }

    public UsersLoginResponse getRolebyUsername(String username) {
        String sql = """      
                    SELECT u.fullName
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

    public List<UsersResponse> getAllUsers() {

        String sql = """
            SELECT u.id,
                   u.fullName,
                   u.email,
                   u.phone,
                   u.username,
                   u.gender,
                   u.avatar,
                   u.status,
                   u.active,
                   r.code AS role
            FROM users u
            LEFT JOIN roles r ON u.role_id = r.id
            ORDER BY u.id DESC
            """;

        return read(session ->
                session.createNativeQuery(sql, Object[].class)
                        .getResultList()
                        .stream()
                        .map(row -> new UsersResponse(
                                ((Number) row[0]).longValue(),
                                (String) row[1],
                                (String) row[2],
                                (String) row[3],
                                (String) row[4],
                                (Boolean) row[5],
                                (String) row[6],
                                row[7] != null ? UserStatus.valueOf(row[7].toString()) : null,
                                (Boolean) row[8],
                                (String) row[9]
                        ))
                        .toList()
        );
    }
}
