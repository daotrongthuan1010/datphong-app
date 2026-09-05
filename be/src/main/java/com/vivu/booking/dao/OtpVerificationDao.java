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

    /** Dung cho AuthServiceImpl: tim OTP moi nhat con hieu luc (chua dung, chua het han logic). */
    public Optional<OtpVerification> findLatestValid(String email, OtpPurposeType purpose) {
        return read(s -> s.createQuery("""
                        from OtpVerification
                        where email = :email and purpose = :purpose and isUsed = false
                        order by createdAt desc
                        """, OtpVerification.class)
                .setParameter("email", email)
                .setParameter("purpose", purpose)
                .setMaxResults(1)
                .uniqueResultOptional());
    }

    /** Vo hieu hoa toan bo OTP cu cung email+purpose (dung khi gui OTP moi). */
    public void invalidateAllForEmail(String email, OtpPurposeType purpose) {
        tx(s -> s.createMutationQuery("""
                        update OtpVerification set isUsed = true
                        where email = :email and purpose = :purpose and isUsed = false
                        """)
                .setParameter("email", email)
                .setParameter("purpose", purpose)
                .executeUpdate());
    }

    public void markUsed(Long id) {
        tx(s -> {
            OtpVerification otp = s.find(OtpVerification.class, id);
            if (otp != null) {
                otp.setIsUsed(true);
                s.merge(otp);
            }
            return null;
        });
    }

    // ---- cac method cu van giu de tuong thich nguoc (nhanh code-Viet truyen EntityManager vao) ----

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

    /** Bulk update: vo hieu hoa toan bo OTP cu cung email+purpose khi gui OTP moi. */
    public int invalidateAllForEmail(String email, OtpPurposeType purpose, EntityManager em) {
        return em.createQuery(
                        "UPDATE OtpVerification o SET o.isUsed = true " +
                                "WHERE o.email = :email AND o.purpose = :purpose AND o.isUsed = false")
                .setParameter("email", email)
                .setParameter("purpose", purpose)
                .executeUpdate();
    }
}
