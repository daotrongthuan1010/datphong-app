package com.vivu.booking.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
    public class UsersLoginRequest {
        private String username;
        private String password;
        /** 2FA: ma 6 so tu Google/Microsoft Authenticator (bat buoc neu tai khoan da bat 2FA). */
        private String totpCode;
    }
