package com.aivle.project.user.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 이메일 전송 서비스.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.email.verification.base-url:http://localhost:8080}")
    private String verificationBaseUrl;

    /**
     * 이메일 인증 링크 전송 (비동기).
     */
    @Async("emailExecutor")
    public void sendVerificationEmail(String toEmail, String verificationToken) {
        try {
            String subject = "Aivle Project - 이메일 인증";
            String verificationUrl = buildVerificationUrl(verificationToken);

            String htmlContent = String.format("""
                <html>
                <body style="font-family: Arial, sans-serif; padding: 20px;">
                    <h2>이메일 인증</h2>
                    <p>안녕하세요, Aivle Project에 가입해 주셔서 감사합니다.</p>
                    <p>아래 링크를 클릭하여 이메일 인증을 완료해주세요.</p>
                    <div style="margin: 30px 0;">
                        <a href="%s" style="background-color: #4CAF50; color: white; padding: 12px 24px; text-decoration: none; border-radius: 4px;">
                            이메일 인증하기
                        </a>
                    </div>
                    <p>이 링크는 30분 후 만료됩니다.</p>
                    <hr>
                    <p style="color: #666; font-size: 12px;">
                        본 메일은 발신 전용입니다.<br>
                        문의사항은 고객센터로 연락해주세요.
                    </p>
                </body>
                </html>
                """, verificationUrl);

            sendHtmlEmail(toEmail, subject, htmlContent);
            log.info("인증 이메일 전송 완료: {}", toEmail);
        } catch (Exception e) {
            log.error("인증 이메일 전송 실패: {}", toEmail, e);
            throw new RuntimeException("이메일 전송 실패", e);
        }
    }

    /**
     * HTML 이메일 전송.
     */
    private void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("이메일 전송 중 오류 발생", e);
        }
    }

    private String buildVerificationUrl(String verificationToken) {
        String baseUrl = verificationBaseUrl.endsWith("/")
            ? verificationBaseUrl.substring(0, verificationBaseUrl.length() - 1)
            : verificationBaseUrl;
        return baseUrl + "/api/auth/verify-email?token=" + verificationToken + "&redirect=true";
    }
}
