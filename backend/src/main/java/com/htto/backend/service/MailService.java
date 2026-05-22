package com.htto.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final JavaMailSender mailSender;
    private final String mailUsername;

    public MailService(
            JavaMailSender mailSender,
            @Value("${spring.mail.username:}") String mailUsername
    ) {
        this.mailSender = mailSender;
        this.mailUsername = mailUsername;
    }

    public boolean sendPasswordResetOtp(String toEmail, String otp) {
        if (!StringUtils.hasText(mailUsername)) {
            log.warn("Password reset email was not sent because MAIL_USERNAME is not configured");
            return false;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailUsername);
        message.setTo(toEmail);
        message.setSubject("Đặt lại mật khẩu hệ thống thi online");
        message.setText("""
                Bạn đã yêu cầu đặt lại mật khẩu cho hệ thống thi online.
                Mã xác thực của bạn là: %s
                Mã này có hiệu lực trong 10 phút.
                Nếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này.
                """.formatted(otp));

        try {
            mailSender.send(message);
            return true;
        } catch (MailException ex) {
            log.warn("Password reset email could not be sent to {}", toEmail, ex);
            return false;
        }
    }
}
