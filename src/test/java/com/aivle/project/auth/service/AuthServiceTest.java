package com.aivle.project.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.aivle.project.auth.dto.LoginRequest;
import com.aivle.project.auth.dto.TokenRefreshRequest;
import com.aivle.project.auth.dto.TokenResponse;
import com.aivle.project.auth.exception.AuthErrorCode;
import com.aivle.project.auth.exception.AuthException;
import com.aivle.project.auth.token.JwtTokenService;
import com.aivle.project.auth.token.RefreshTokenCache;
import com.aivle.project.user.security.CustomUserDetails;
import com.aivle.project.user.security.CustomUserDetailsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	@Mock
	private AuthenticationManager authenticationManager;

	@Mock
	private JwtTokenService jwtTokenService;

	@Mock
	private RefreshTokenService refreshTokenService;

	@Mock
	private CustomUserDetailsService userDetailsService;

	@Test
	@DisplayName("로그인 성공 시 액세스/리프레시 토큰을 반환한다")
	void login_shouldReturnTokenResponse() {
		// given: 로그인 요청과 인증 성공 상태를 준비
		AuthService authService = new AuthService(authenticationManager, jwtTokenService, refreshTokenService, userDetailsService);

		LoginRequest request = new LoginRequest();
		request.setEmail("user@example.com");
		request.setPassword("password");

		CustomUserDetails userDetails = mock(CustomUserDetails.class);

		Authentication authentication = mock(Authentication.class);
		when(authentication.getPrincipal()).thenReturn(userDetails);
		when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);

		when(jwtTokenService.createAccessToken(userDetails, "default")).thenReturn("access");
		when(jwtTokenService.createRefreshToken()).thenReturn("refresh");
		when(jwtTokenService.getAccessTokenExpirationSeconds()).thenReturn(1800L);
		when(jwtTokenService.getRefreshTokenExpirationSeconds()).thenReturn(604800L);

		// when: 로그인을 수행
		TokenResponse response = authService.login(request, "127.0.0.1");

		// then: 토큰 응답과 Refresh 저장 동작을 검증
		assertThat(response.accessToken()).isEqualTo("access");
		assertThat(response.refreshToken()).isEqualTo("refresh");
		verify(refreshTokenService).storeToken(eq(userDetails), eq("refresh"), eq("default"), eq(request.getDeviceInfo()), eq("127.0.0.1"));
	}

	@Test
	@DisplayName("리프레시 요청 시 새 토큰 쌍을 반환한다")
	void refresh_shouldReturnNewTokens() {
		// given: 리프레시 토큰 회전 결과와 활성 사용자 상태를 준비
		AuthService authService = new AuthService(authenticationManager, jwtTokenService, refreshTokenService, userDetailsService);

		TokenRefreshRequest request = new TokenRefreshRequest();
		request.setRefreshToken("old-token");

		RefreshTokenCache rotated = new RefreshTokenCache(
			"new-token",
			"user-uuid",
			"user@example.com",
			"device-1",
			"ios",
			"127.0.0.1",
			1L,
			2L,
			1L
		);
		when(refreshTokenService.rotateToken("old-token", "new-token")).thenReturn(rotated);
		when(jwtTokenService.createRefreshToken()).thenReturn("new-token");

		CustomUserDetails userDetails = mock(CustomUserDetails.class);
		when(userDetails.isEnabled()).thenReturn(true);
		when(userDetailsService.loadUserByUsername("user@example.com")).thenReturn(userDetails);
		when(jwtTokenService.createAccessToken(userDetails, "device-1")).thenReturn("access");
		when(jwtTokenService.getAccessTokenExpirationSeconds()).thenReturn(1800L);
		when(jwtTokenService.getRefreshTokenExpirationSeconds()).thenReturn(604800L);

		// when: 리프레시를 수행
		TokenResponse response = authService.refresh(request);

		// then: 새 액세스/리프레시 토큰을 반환한다
		assertThat(response.accessToken()).isEqualTo("access");
		assertThat(response.refreshToken()).isEqualTo("new-token");
	}

	@Test
	@DisplayName("비활성 사용자면 리프레시가 실패한다")
	void refresh_shouldThrowWhenUserDisabled() {
		// given: 리프레시 회전 결과와 비활성 사용자 상태를 준비
		AuthService authService = new AuthService(authenticationManager, jwtTokenService, refreshTokenService, userDetailsService);

		TokenRefreshRequest request = new TokenRefreshRequest();
		request.setRefreshToken("old-token");

		RefreshTokenCache rotated = new RefreshTokenCache(
			"new-token",
			"user-uuid",
			"user@example.com",
			"device-1",
			"ios",
			"127.0.0.1",
			1L,
			2L,
			1L
		);
		when(refreshTokenService.rotateToken("old-token", "new-token")).thenReturn(rotated);
		when(jwtTokenService.createRefreshToken()).thenReturn("new-token");

		CustomUserDetails userDetails = mock(CustomUserDetails.class);
		when(userDetails.isEnabled()).thenReturn(false);
		when(userDetailsService.loadUserByUsername("user@example.com")).thenReturn(userDetails);

		// when & then: 비활성 사용자는 예외가 발생한다
		assertThatThrownBy(() -> authService.refresh(request))
			.isInstanceOf(AuthException.class);
	}

	@Test
	@DisplayName("인증 실패 시 로그인 요청이 거절된다")
	void login_shouldThrowWhenAuthenticationFails() {
		// given: 인증 실패 상태를 준비
		AuthService authService = new AuthService(authenticationManager, jwtTokenService, refreshTokenService, userDetailsService);

		LoginRequest request = new LoginRequest();
		request.setEmail("user@example.com");
		request.setPassword("wrong-password");

		when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
			.thenThrow(new BadCredentialsException("Bad credentials"));

		// when & then: 인증 실패면 AuthException이 발생한다
		assertThatThrownBy(() -> authService.login(request, "127.0.0.1"))
			.isInstanceOf(AuthException.class);

		verifyNoInteractions(jwtTokenService, refreshTokenService);
	}

	@Test
	@DisplayName("리프레시 토큰이 유효하지 않으면 예외가 전파된다")
	void refresh_shouldThrowWhenRefreshTokenInvalid() {
		// given: 리프레시 토큰 검증 실패 상태를 준비
		AuthService authService = new AuthService(authenticationManager, jwtTokenService, refreshTokenService, userDetailsService);

		TokenRefreshRequest request = new TokenRefreshRequest();
		request.setRefreshToken("invalid-refresh");

		when(jwtTokenService.createRefreshToken()).thenReturn("new-token");
		when(refreshTokenService.rotateToken("invalid-refresh", "new-token"))
			.thenThrow(new AuthException(AuthErrorCode.INVALID_REFRESH_TOKEN));

		// when & then: 예외가 그대로 전달된다
		assertThatThrownBy(() -> authService.refresh(request))
			.isInstanceOf(AuthException.class)
			.hasMessage(AuthErrorCode.INVALID_REFRESH_TOKEN.getMessage());
	}
}
