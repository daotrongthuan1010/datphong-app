package com.vivu.booking.entity;

import com.vivu.booking.enums.WalletOwnerType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "wallets", uniqueConstraints = @UniqueConstraint(columnNames = {"owner_id", "owner_type"}))
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Có thể là user_id hoặc host_id tùy owner_type
    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "owner_type", nullable = false, columnDefinition = "wallet_owner_type")
    private WalletOwnerType ownerType;

    @Column(nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(nullable = false, length = 5)
    @Builder.Default
    private String currency = "VND";
}
