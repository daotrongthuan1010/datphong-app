package com.vivu.booking.dao;

import com.vivu.booking.config.HibernateConfig;
import com.vivu.booking.entity.Role;
import com.vivu.booking.entity.User;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class RoleDao extends BaseDao<Role,Long> {

    public RoleDao() {
        super(Role.class);
    }

    /** Dùng để lấy role mặc định ("user") gán cho tài khoản tự đăng ký. */
    public java.util.Optional<Role> findByCode(String code) {
        return read(s -> s.createQuery("from Role where code = :code", Role.class)
                .setParameter("code", code)
                .uniqueResultOptional());
    }
    public void addRole(Long userId, Long roleId) {
        Transaction transaction = null;

        try (Session session = HibernateConfig.getSessionFactory().openSession()) {

            transaction = session.beginTransaction();

            String sql = """
            INSERT INTO user_roles (user_id, role_id)
            VALUES (:userId, :roleId)
            ON CONFLICT (user_id, role_id) DO NOTHING
            """;

            session.createNativeQuery(sql)
                    .setParameter("userId", userId)
                    .setParameter("roleId", roleId)
                    .executeUpdate();

            transaction.commit();

        } catch (Exception e) {

            if (transaction != null) {
                transaction.rollback();
            }

            throw new RuntimeException(
                    "Không thể thêm role cho user",
                    e
            );
        }
    }
    public Set<Role> findByIds(Set<Long> ids) {

        if (ids == null || ids.isEmpty()) {
            return new HashSet<>();
        }

        try (Session session = HibernateConfig.getSessionFactory().openSession()) {

            String hql = """
                    SELECT r
                    FROM Role r
                    WHERE r.id IN (:ids)
                    """;

            List<Role> roles = session.createQuery(hql, Role.class)
                    .setParameterList("ids", ids)
                    .getResultList();

            return new HashSet<>(roles);

        } catch (Exception e) {

            throw new RuntimeException(
                    "Không thể lấy danh sách role",
                    e
            );
        }
    }
}
