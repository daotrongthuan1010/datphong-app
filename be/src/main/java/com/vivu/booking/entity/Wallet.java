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
@Table(name = "wallets", uniqueConstraints = {@UniqueConstraint(name = "uk_wallet_user", columnNames = "user_id")})
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Có thể là user_id hoặc host_id tùy owner_type
    @OneToOne(fetch = FetchType.LAZY,optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(nullable = false, length = 5)
    @Builder.Default
    private String currency = "VND";
    // Chống 2 request cùng sửa balance gây ghi đè
    @Version
    private Long version;
}
