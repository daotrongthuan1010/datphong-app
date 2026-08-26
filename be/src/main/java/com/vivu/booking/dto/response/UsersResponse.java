package com.vivu.booking.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.vivu.booking.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UsersResponse {
    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private String username;
    private Boolean gender;
    private String avatar;
    private UserStatus status;
    private Boolean active;
    private String role;
}
