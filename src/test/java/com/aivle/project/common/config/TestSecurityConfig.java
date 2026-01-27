package com.aivle.project.common.config;

import com.aivle.project.auth.service.TurnstileService;
import com.aivle.project.auth.token.JwtKeyProvider;
import com.aivle.project.auth.token.JwtProperties;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Properties;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * 테스트용 JWT 설정 (파일 시스템 접근 제거).
 */
@TestConfiguration
public class TestSecurityConfig {

	private static final KeyPair KEY_PAIR = generateKeyPair();

	@Bean
	@Primary
	public JwtKeyProvider testJwtKeyProvider(JwtProperties jwtProperties) {
		return new JwtKeyProvider(jwtProperties) {
			@Override
			public RSAPrivateKey loadPrivateKey() {
				return (RSAPrivateKey) KEY_PAIR.getPrivate();
			}

			@Override
			public RSAPublicKey loadPublicKey() {
				return (RSAPublicKey) KEY_PAIR.getPublic();
			}
		};
	}

	@Bean
	@Primary
	public JavaMailSender testJavaMailSender() {
		JavaMailSender mailSender = Mockito.mock(JavaMailSender.class);
		Mockito.when(mailSender.createMimeMessage())
			.thenAnswer(invocation -> new MimeMessage(Session.getDefaultInstance(new Properties())));
		// 테스트에서는 실제 메일 발송을 막기 위해 모킹한다.
		return mailSender;
	}

	@Bean
	@Primary
	public TurnstileService testTurnstileService() {
		TurnstileService turnstileService = Mockito.mock(TurnstileService.class);
		// 기본적으로 Turnstile 검증을 통과하도록 설정
		Mockito.when(turnstileService.verifyTokenSync(Mockito.anyString(), Mockito.any()))
			.thenReturn(true);
		return turnstileService;
	}

	private static KeyPair generateKeyPair() {
		try {
			KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
			generator.initialize(2048);
			return generator.generateKeyPair();
		} catch (Exception ex) {
			throw new IllegalStateException("테스트용 RSA 키 생성에 실패했습니다", ex);
		}
	}
}
