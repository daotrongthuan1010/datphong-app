package com.vivu.booking.dao;

import com.vivu.booking.entity.OtpVerification;
import com.vivu.booking.enums.OtpPurposeType;
import jakarta.persistence.EntityManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class OtpVerificationDao extends BaseDao<OtpVerification, Long> {
    public OtpVerificationDao(Class<OtpVerification> entityClass) {
        super(entityClass);
    }

    public Optional<OtpVerification> findValidOtp(String email, OtpPurposeType purpose,
                                                  String otpCode, EntityManager em) {
        List<OtpVerification> result = em.createQuery(
                        "SELECT o FROM OtpVerification o " +
                                "WHERE o.email = :email AND o.purpose = :purpose AND o.otpCode = :code " +
                                "AND o.isUsed = false AND o.expiresAt > :now " +
                                "ORDER BY o.createdAt DESC", OtpVerification.class)
                .setParameter("email", email)
                .setParameter("purpose", purpose)
                .setParameter("code", otpCode)
                .setParameter("now", LocalDateTime.now())
                .setMaxResults(1)
                .getResultList();
        return result.stream().findFirst();
    }

    /** Bulk update: vô hiệu hoá toàn bộ OTP cũ cùng email+purpose khi gửi OTP mới. */
    public int invalidateAllForEmail(String email, OtpPurposeType purpose, EntityManager em) {
        return em.createQuery(
                        "UPDATE OtpVerification o SET o.isUsed = true " +
                                "WHERE o.email = :email AND o.purpose = :purpose AND o.isUsed = false")
                .setParameter("email", email)
                .setParameter("purpose", purpose)
                .executeUpdate();
    }
}
