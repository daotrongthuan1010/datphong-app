package com.vivu.booking.dto.request;

import com.vivu.booking.entity.Role;
import com.vivu.booking.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsersResquest {
    private String fullName;
    private String email;
    private String phone;
    private String username;
    private String password;
    private Boolean gender;
    private String avatar;
    private UserStatus status;
    private Boolean active;
    private Long roleId;
}
