package com.vivu.booking.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UsersLoginResponse {
       private String fullName;
       private String username;
       @JsonIgnore //ẩn password
       private String password;
       private Set<String> role;
}
