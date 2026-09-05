package com.vivu.booking.mapper;

import com.vivu.booking.dto.request.UserImportRequest;
import com.vivu.booking.entity.Role;
import com.vivu.booking.entity.User;
import org.mindrot.jbcrypt.BCrypt;

import java.util.Set;

public final class UserImportMapper {

    private UserImportMapper() {
    }

    public static User toEntity(
            UserImportRequest request,
            Set<Role> roles
    ) {

        User user = new User();

        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setUsername(request.getUsername());

        // Mã hóa mật khẩu
        if (request.getPassword() != null
                && !request.getPassword().isBlank()) {

            user.setPassword(
                    BCrypt.hashpw(
                            request.getPassword(),
                            BCrypt.gensalt()
                    )
            );
        }

        user.setGender(request.getGender());
        user.setAvatar(request.getAvatar());
        user.setStatus(request.getStatus());
        user.setActive(request.getActive());

        // Gán Role đã lấy từ database
        user.setRole(roles);

        return user;
    }
}