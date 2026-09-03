package com.vivu.booking.service.impl;

import com.vivu.booking.enums.OtpPurposeType;
import com.vivu.booking.service.EmailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** TODO: thay bằng SMTP/JavaMailSender thật khi có cấu hình mail server. */
public class ConsoleEmailSender implements EmailSender {
    private static final Logger log = LoggerFactory.getLogger(ConsoleEmailSender.class);

    @Override
    public void sendOtpEmail(String toEmail, String otpCode, OtpPurposeType purpose) {
        log.info("[DEV-ONLY][KHÔNG PHẢI EMAIL THẬT] Gửi OTP tới {} (purpose={}): {}", toEmail, purpose, otpCode);
    }
}
