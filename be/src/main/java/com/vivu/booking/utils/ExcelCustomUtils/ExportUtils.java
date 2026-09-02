package com.vivu.booking.utils.ExcelCustomUtils;

import com.vivu.booking.entity.Role;
import com.vivu.booking.entity.User;

import java.util.List;
import java.util.stream.Collectors;

public final class ExportUtils {
    private ExportUtils() {}
    public static byte[] exportUser(List<User> users) {
        String[] headers = {
                "Full Name",
                "Email",
                "Phone",
                "Username",
                "Gender",
                "Avatar",
                "Status",
                "Role"
        };
        List<String[]> data = users.stream()
                .map(u -> new String[]{
                        u.getFullName(),
                        u.getEmail(),
                        u.getPhone(),
                        u.getUsername(),
                        u.getGender() != null ? (u.getGender() ? "Nam" : "Nữ") : "",
                        u.getAvatar(),
                        u.getStatus() != null ? u.getStatus().toString() : "",
                        u.getRole() != null
                                ? u.getRole()
                                .stream()
                                .map(Role::getCode)
                                .collect(Collectors.joining(", "))
                                : ""
                }).toList();
        return ExcelUtils.export("User", headers, data);
    }

}
