package com.vivu.booking.entity;

import com.vivu.booking.enums.KycStatusType;
import com.vivu.booking.enums.UserStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(unique = true, length = 20)
    private String phone;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "avatar_url", length = 255)
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "user_status")
    @Builder.Default
    private UserStatus status = UserStatus.PENDING_VERIFY;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "kyc_status", nullable = false, columnDefinition = "kyc_status_type")
    @Builder.Default
    private KycStatusType kycStatus = KycStatusType.NONE;

    @Column(name = "language_pref", length = 5)
    @Builder.Default
    private String languagePref = "vi";

    @Column(name = "currency_pref", length = 5)
    @Builder.Default
    private String currencyPref = "VND";
}
