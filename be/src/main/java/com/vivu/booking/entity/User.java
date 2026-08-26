package com.vivu.booking.entity;

import com.vivu.booking.enums.UserStatus;
import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "users")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class User extends BaseEntity{
       @Id
       @GeneratedValue(strategy = GenerationType.IDENTITY)
       private Long id;
       private String fullName;
       private String email;
       private String phone;
       private String username;
       private String password;
       private Boolean gender;
       private String avatar;
       @Enumerated(EnumType.STRING)
       @Column(nullable = false, length = 20)
       @Builder.Default
       private UserStatus status=UserStatus.ACTIVE;
       private Boolean active;
       @ManyToOne(fetch = FetchType.EAGER)
       @JoinColumn(name = "role_id")
       private Role role;
}
