package com.vivu.booking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleCreateRequest {

    @NotBlank(message = "code không được để trống")
    @Size(max = 50, message = "code không được quá 50 ký tự")
    @jakarta.validation.constraints.Pattern(
            regexp = "^[a-z0-9_]+$",
            message = "code chỉ được chứa chữ thường, số và dấu gạch dưới (VD: content_moderator)")
    private String code;

    @NotBlank(message = "name không được để trống")
    @Size(max = 100, message = "name không được quá 100 ký tự")
    private String name;

    @Size(max = 255, message = "description không được quá 255 ký tự")
    private String description;
}
