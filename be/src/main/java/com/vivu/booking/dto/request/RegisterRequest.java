package com.vivu.booking.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * Dùng cho POST /api/auth/register - khác với UsersResquest (Admin tạo/sửa user,
 * cho chọn role/status/active tuỳ ý). Người tự đăng ký KHÔNG được chọn role -
 * mặc định gán role "user" ở tầng Service.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {

    @NotBlank(message = "Họ và tên không được để trống")
    @Size(max = 150, message = "Họ và tên không được quá 150 ký tự")
    private String fullName;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    @Size(max = 150, message = "Email không được quá 150 ký tự")
    private String email;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^0[0-9]{9}$", message = "Số điện thoại phải gồm 10 chữ số và bắt đầu bằng 0")
    private String phone;

    @NotBlank(message = "Username không được để trống")
    @Size(min = 4, max = 50, message = "Username phải từ 4 đến 50 ký tự")
    @Pattern(regexp = "^[a-zA-Z0-9._]+$", message = "Username chỉ được chứa chữ cái, số, dấu chấm và dấu gạch dưới")
    private String username;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 6, max = 100, message = "Mật khẩu phải từ 6 đến 100 ký tự")
    private String password;

    @NotBlank(message = "Vui lòng nhập lại mật khẩu")
    private String confirmPassword;

    private Boolean gender;

    @AssertTrue(message = "Mật khẩu nhập lại không khớp")
    public boolean isPasswordMatching() {
        if (password == null || confirmPassword == null) return true;
        return password.equals(confirmPassword);
    }
}
