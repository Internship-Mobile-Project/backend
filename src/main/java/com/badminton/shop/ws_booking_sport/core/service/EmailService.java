// ...existing code...
package com.badminton.shop.ws_booking_sport.core.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendVerificationCode(String to, String code) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(to);
        msg.setSubject("[Sport Booking] Xác thực tài khoản");
        msg.setText("Mã xác thực của bạn: " + code + "\n\nNếu bạn không yêu cầu mã này, hãy bỏ qua email này.");
        mailSender.send(msg);
    }
}

