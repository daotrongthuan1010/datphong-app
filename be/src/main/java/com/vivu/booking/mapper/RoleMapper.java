package com.vivu.booking.mapper;

import com.vivu.booking.dto.request.RoleCreateRequest;
import com.vivu.booking.dto.response.RoleResponse;
import com.vivu.booking.entity.Role;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RoleMapper {

    public static Role toEntity(RoleCreateRequest req) {
        Role role = new Role();
        role.setCode(req.getCode());
        role.setName(req.getName());
        role.setDescription(req.getDescription());
        return role;
    }

    public static RoleResponse toResponse(Role e) {
        return RoleResponse.builder()
                .id(e.getId())
                .code(e.getCode())
                .name(e.getName())
                .description(e.getDescription())
                .build();
    }
}
