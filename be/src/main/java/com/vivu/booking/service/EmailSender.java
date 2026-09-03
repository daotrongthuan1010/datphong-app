package com.vivu.booking.service;

import com.vivu.booking.enums.OtpPurposeType;

/**
 * Trừu tượng hoá việc gửi email OTP. Project chưa có dependency mail (JavaMailSender/SMTP)
 * nào được cấu hình, nên tạm dùng ConsoleEmailSender (chỉ log ra console) - khi có
 * thông tin SMTP thật, chỉ cần viết implementation khác, KHÔNG cần sửa AuthService.
 */
public interface EmailSender {
    void sendOtpEmail(String toEmail, String otpCode, OtpPurposeType purpose);
}
