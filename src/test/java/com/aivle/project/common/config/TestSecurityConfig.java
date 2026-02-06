package com.aivle.project.common.config;

import com.aivle.project.auth.service.TurnstileService;
import com.aivle.project.auth.token.JwtKeyProvider;
import com.aivle.project.auth.token.JwtProperties;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

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
