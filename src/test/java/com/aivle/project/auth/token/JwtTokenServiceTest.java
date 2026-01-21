package com.aivle.project.auth.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.aivle.project.user.security.CustomUserDetails;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

@DisplayName("JWT 토큰 서비스 단위 테스트")
class JwtTokenServiceTest {

	@Test
	@DisplayName("Access Token 생성: ROLE_ 접두사 제거 및 Claims(email, deviceId 등) 설정 검증")
	void createAccessToken_shouldStripRolePrefixAndSetClaims() {
		// Given: 테스트를 위한 속성 및 Mock 설정
		JwtProperties properties = new JwtProperties();
		properties.setIssuer("project-local");
		properties.getAccessToken().setExpiration(1800);
		properties.getRefreshToken().setExpiration(604800);
		properties.getKeys().setCurrentKid("kid-1");

		// JwtEncoder를 가로채기 위한 Stub 객체 생성
		CapturingJwtEncoder encoder = new CapturingJwtEncoder();
		JwtTokenService tokenService = new JwtTokenService(encoder, properties);

		// UserDetails Mock 설정
		CustomUserDetails userDetails = mock(CustomUserDetails.class);
		UUID uuid = UUID.randomUUID();
		when(userDetails.getUuid()).thenReturn(uuid);
		when(userDetails.getUsername()).thenReturn("user@example.com");

		Collection<? extends GrantedAuthority> authorities = List.of(
				new SimpleGrantedAuthority("ROLE_USER"),
				new SimpleGrantedAuthority("ROLE_ADMIN")
		);
		doReturn(authorities).when(userDetails).getAuthorities();

		// When: Access Token 생성 메서드 호출
		String token = tokenService.createAccessToken(userDetails, "device-1");

		// Then: 결과 및 Claims 검증
		assertThat(token).isEqualTo("token-value"); // Encoder Stub이 반환하는 고정값

		JwtClaimsSet claims = encoder.getClaims();

		// Issuer("iss") 검증 - 편의 메서드 대신 Claims Map에서 직접 조회하여 검증
		assertThat(claims.getClaims().get("iss")).isEqualTo("project-local");
		assertThat(claims.getSubject()).isEqualTo(uuid.toString());
		assertThat(claims.getClaims().get("email")).isEqualTo("user@example.com");

		// 권한 목록에서 'ROLE_' 접두사가 제거되었는지 확인
		@SuppressWarnings("unchecked")
		List<String> roles = (List<String>) claims.getClaims().get("roles");
		assertThat(roles).containsExactlyInAnyOrder("USER", "ADMIN");

		assertThat(claims.getClaims().get("deviceId")).isEqualTo("device-1");
		assertThat(encoder.getHeader().getKeyId()).isEqualTo("kid-1");
	}

	@Test
	@DisplayName("Refresh Token 생성: Base64 URL-safe 문자열 반환 검증")
	void createRefreshToken_shouldReturnBase64UrlToken() {
		// Given
		JwtProperties properties = new JwtProperties();
		properties.setIssuer("project-local");
		properties.getAccessToken().setExpiration(1800);
		properties.getRefreshToken().setExpiration(604800);

		CapturingJwtEncoder encoder = new CapturingJwtEncoder();
		JwtTokenService tokenService = new JwtTokenService(encoder, properties);

		// When
		String token = tokenService.createRefreshToken();

		// Then: Base64 URL-safe 포맷 검증
		assertThat(token).isNotBlank();
		assertThat(token).doesNotContain("="); // 패딩 문자 제거 확인
		assertThat(token).matches("[A-Za-z0-9_-]+"); // URL-safe 문자 집합 확인
	}

	/**
	 * 테스트용 Stub 클래스
	 * 실제 JWT 서명 과정을 수행하지 않고, 전달된 파라미터(Header, Claims)를 캡처하여 검증하기 위함입니다.
	 */
	private static class CapturingJwtEncoder implements JwtEncoder {

		private JwsHeader header;
		private JwtClaimsSet claims;

		@Override
		public Jwt encode(JwtEncoderParameters parameters) {
			this.header = parameters.getJwsHeader();
			this.claims = parameters.getClaims();
			Instant now = Instant.now();
			// 실제 서명 대신 고정된 더미 토큰 반환
			return new Jwt("token-value", now, now.plusSeconds(60), header.getHeaders(), claims.getClaims());
		}

		public JwsHeader getHeader() {
			return header;
		}

		public JwtClaimsSet getClaims() {
			return claims;
		}
	}
}