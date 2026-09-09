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
        /**
         * @deprecated Dang nhap da chia 2 buoc: POST /api/auth/login (username+password)
         * tra ve loginToken + QR (lan dau), roi POST /api/auth/login/2fa (loginToken+code).
         * Truong nay giu lai tuong thich nguoc cho may FE chua update, khong dung nua.
         */
        @Deprecated
        private String totpCode;
    }
