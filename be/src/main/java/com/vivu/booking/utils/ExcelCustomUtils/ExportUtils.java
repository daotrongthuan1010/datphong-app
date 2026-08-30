package com.vivu.booking.utils.ExcelCustomUtils;

import com.vivu.booking.entity.User;

import java.util.List;

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
        };
        List<String[]> data = users.stream()
                .map(u -> new String[]{
                        u.getFullName(),
                        u.getEmail(),
                        u.getPhone(),
                        u.getUsername(),
                        u.getGender() != null ? (u.getGender() ? "Nam" : "Nữ") : "",
                        u.getAvatar(),
                        u.getStatus() != null ? u.getStatus().toString() : ""
                }).toList();
        return ExcelUtils.export("User", headers, data);
    }

}
