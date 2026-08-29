package com.vivu.booking.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.vivu.booking.enums.UserStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UsersResponse {
    private Long id;
    @NotBlank
    private String fullName;
    private String email;
    private String phone;
    private String username;
    private Boolean gender;
    private String avatar;
    private UserStatus status;
    private Boolean active;
    private Set<String> role;
}
