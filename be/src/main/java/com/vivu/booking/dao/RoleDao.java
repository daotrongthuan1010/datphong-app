package com.vivu.booking.dao;

import com.vivu.booking.config.HibernateConfig;
import com.vivu.booking.entity.Role;
import com.vivu.booking.entity.User;
import org.hibernate.Session;
import org.hibernate.Transaction;

public class RoleDao extends BaseDao<Role,Long> {

    public RoleDao() {
        super(Role.class);
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
}
