package com.vivu.booking.dto.request;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UsersLoginRequest {
    private String username;
    private String password;
}
