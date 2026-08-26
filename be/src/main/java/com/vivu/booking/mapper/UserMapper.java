package com.vivu.booking.mapper;

import com.vivu.booking.dto.request.UsersResquest;
import com.vivu.booking.dto.response.UsersResponse;
import com.vivu.booking.entity.Role;
import com.vivu.booking.entity.User;
import com.vivu.booking.enums.UserStatus;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class UserMapper {
    public static User toEntity(UsersResquest req,Role role) {
        return User.builder()
                .fullName(req.getFullName())
                .email(req.getEmail())
                .phone(req.getPhone())
                .username(req.getUsername())
                .password(req.getPassword())
                .gender(true)
                .avatar(req.getAvatar())
                .status(req.getStatus()!=null?req.getStatus():UserStatus.ACTIVE)
                .active(req.getActive()!=null?req.getActive():true)
                .role(role)
                .build();
    }
    public static UsersResponse toResponse(User e) {
        return UsersResponse.builder()
                .id(e.getId())
                .fullName(e.getFullName())
                .email(e.getEmail())
                .phone(e.getPhone())
                .username(e.getUsername())
                .gender(e.getGender())
                .avatar(e.getAvatar())
                .status(e.getStatus())
                .active(e.getActive())
                .role(e.getRole() != null ? e.getRole().getCode() : null)
                .build();
    }
}