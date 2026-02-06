package com.aivle.project.common.config;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * 테스트 환경에서 메일 발송 빈을 대체한다.
 */
@Configuration
@Profile("test")
public class TestMailConfig {

	@Bean
	@Primary
	public JavaMailSender testJavaMailSender() {
		JavaMailSender mailSender = Mockito.mock(JavaMailSender.class);
		Mockito.when(mailSender.createMimeMessage())
			.thenAnswer(invocation -> new MimeMessage(Session.getDefaultInstance(new Properties())));
		return mailSender;
	}
}
