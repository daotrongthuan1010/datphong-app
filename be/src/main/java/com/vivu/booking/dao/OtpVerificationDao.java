package com.vivu.booking.dao;

import com.vivu.booking.config.HibernateConfig;
import com.vivu.booking.entity.OtpVerification;
import com.vivu.booking.enums.OtpPurposeType;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class OtpVerificationDao extends BaseDao<OtpVerification, Long> {

    public OtpVerificationDao() {
        super(OtpVerification.class);
    }

    /**
     * Lấy OTP mới nhất còn hiệu lực.
     */
    public Optional<OtpVerification> findLatestValid(
            String email,
            OtpPurposeType purpose) {

        try (Session session = HibernateConfig.getSessionFactory().openSession()) {

            List<OtpVerification> result = session.createQuery(
                            """
                            SELECT o
                            FROM OtpVerification o
                            WHERE o.email = :email
                              AND o.purpose = :purpose
                              AND o.isUsed = false
                              AND o.expiresAt > :now
                            ORDER BY o.createdAt DESC
                            """,
                            OtpVerification.class
                    )
                    .setParameter("email", email)
                    .setParameter("purpose", purpose)
                    .setParameter("now", LocalDateTime.now())
                    .setMaxResults(1)
                    .getResultList();

            return result.stream().findFirst();
        }
    }

    /**
     * Vô hiệu hóa toàn bộ OTP cũ
     * của cùng email + purpose.
     */
    public int invalidateAllForEmail(
            String email,
            OtpPurposeType purpose) {

        Transaction transaction = null;

        try (Session session = HibernateConfig.getSessionFactory().openSession()) {

            transaction = session.beginTransaction();

            int count = session.createMutationQuery(
                            """
                            UPDATE OtpVerification o
                            SET o.isUsed = true
                            WHERE o.email = :email
                              AND o.purpose = :purpose
                              AND o.isUsed = false
                            """
                    )
                    .setParameter("email", email)
                    .setParameter("purpose", purpose)
                    .executeUpdate();

            transaction.commit();

            return count;

        } catch (Exception e) {

            if (transaction != null) {
                transaction.rollback();
            }

            throw e;
        }
    }

    /**
     * Đánh dấu OTP đã được sử dụng.
     */
    public void markUsed(Long id) {

        Transaction transaction = null;

        try (Session session = HibernateConfig.getSessionFactory().openSession()) {

            transaction = session.beginTransaction();

            session.createMutationQuery(
                            """
                            UPDATE OtpVerification o
                            SET o.isUsed = true
                            WHERE o.id = :id
                            """
                    )
                    .setParameter("id", id)
                    .executeUpdate();

            transaction.commit();

        } catch (Exception e) {

            if (transaction != null) {
                transaction.rollback();
            }

            throw e;
        }
    }
}