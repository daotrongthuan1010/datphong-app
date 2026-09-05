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
       private Long id;
       private String fullName;
       private String username;
       // Đề xuất của Việt - nên bỏ trường password vi ly do bảo mật >< An
       @JsonIgnore //ẩn password
       private String password;
       private Set<String> role;
}
