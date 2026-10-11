package com.moodandmove.mail.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendVerificationCode(String toEmail, String code) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("[Mood&Move] 이메일 인증번호 안내");

        message.setText(
                "안녕하세요. Mood&Move입니다.\n\n"
                        + "회원가입 인증번호는 [" + code + "] 입니다.\n\n"
                        + "인증번호는 발급 후 5분 동안 유효합니다.\n"
                        + "본인이 요청하지 않았다면 이 메일을 무시해 주세요."
        );

        mailSender.send(message);
    }
}