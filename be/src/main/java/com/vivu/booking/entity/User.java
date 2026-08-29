package com.vivu.booking.entity;

import com.vivu.booking.enums.UserStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.HashSet;
import java.util.Set;


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
       @ManyToMany(fetch = FetchType.LAZY)
       @JoinTable(
               name = "user_roles",
               joinColumns = @JoinColumn(name = "user_id"),
               inverseJoinColumns = @JoinColumn(name = "role_id")
       )
       @Builder.Default
       private Set<Role> role= new HashSet<>();

}
