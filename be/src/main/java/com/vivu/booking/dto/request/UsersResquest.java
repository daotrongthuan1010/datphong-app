package com.vivu.booking.dto.request;

import com.vivu.booking.enums.UserStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsersResquest {
    @NotBlank(message = "ten loi")
    private String fullName;
    private String email;
    private String phone;
    private String username;
    private String password;
    private Boolean gender;
    private String avatar;
    private UserStatus status;
    private Boolean active;
    private Set<Long> roleId;
}
