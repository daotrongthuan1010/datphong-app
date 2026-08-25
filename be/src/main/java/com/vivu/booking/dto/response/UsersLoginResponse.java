package com.vivu.booking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UsersLoginResponse {
       private String fullName;
       private String username;
       private String password;
       private String role;
}
