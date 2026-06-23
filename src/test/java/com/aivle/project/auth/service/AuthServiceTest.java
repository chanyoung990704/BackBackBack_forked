package com.aivle.project.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.aivle.project.auth.dto.AuthLoginResponse;
import com.aivle.project.auth.dto.LoginRequest;
import com.aivle.project.auth.dto.PasswordChangeRequest;
import com.aivle.project.auth.dto.TokenRefreshRequest;
import com.aivle.project.auth.dto.TokenResponse;
import com.aivle.project.auth.exception.AuthErrorCode;
import com.aivle.project.auth.exception.AuthException;
import com.aivle.project.auth.token.JwtTokenService;
import com.aivle.project.auth.token.RefreshTokenCache;
import com.aivle.project.auth.user.entity.UserEntity;
import com.aivle.project.auth.user.security.CustomUserDetails;
import com.aivle.project.auth.user.security.CustomUserDetailsService;
import com.aivle.project.auth.user.service.UserDomainService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	@Mock
	private AuthenticationManager authenticationManager;

	@Mock
	private JwtTokenService jwtTokenService;

	@Mock
	private RefreshTokenService refreshTokenService;

	@Mock
	private AccessTokenBlacklistService accessTokenBlacklistService;

	@Mock
	private LoginAttemptService loginAttemptService;

	@Mock
	private CustomUserDetailsService userDetailsService;

	@Mock
	private UserDomainService userDomainService;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private com.aivle.project.auth.user.mapper.UserMapper userMapper;

	private AuthService newAuthService() {
		return new AuthService(
			authenticationManager,
			jwtTokenService,
			refreshTokenService,
			accessTokenBlacklistService,
			loginAttemptService,
			userDetailsService,
			userDomainService,
			passwordEncoder,
			userMapper
		);
	}

	@Test
	@DisplayName("로그인 성공 시 액세스/리프레시 토큰과 사용자 정보를 반환한다")
	void login_shouldReturnTokenResponse() {
		// given: 로그인 요청과 인증 성공 상태를 준비
		AuthService authService = newAuthService();

		LoginRequest request = new LoginRequest();
		request.setEmail("user@example.com");
		request.setPassword("password");

		CustomUserDetails userDetails = mock(CustomUserDetails.class);
		when(userDetails.getAuthorities()).thenReturn((List) List.of(new SimpleGrantedAuthority("ROLE_USER")));

		Authentication authentication = mock(Authentication.class);
		when(authentication.getPrincipal()).thenReturn(userDetails);
		when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);

		when(jwtTokenService.createAccessToken(userDetails, "default")).thenReturn("access");
		when(jwtTokenService.createRefreshToken()).thenReturn("refresh");
		when(jwtTokenService.getAccessTokenExpirationSeconds()).thenReturn(1800L);
		when(jwtTokenService.getRefreshTokenExpirationSeconds()).thenReturn(604800L);

		com.aivle.project.auth.user.dto.UserSummaryDto userSummaryDto = new com.aivle.project.auth.user.dto.UserSummaryDto(
			UUID.randomUUID().toString(),
			"user@example.com",
			"홍길동",
			com.aivle.project.auth.user.entity.RoleName.ROLE_USER
		);
		when(userMapper.toSummaryDto(eq(userDetails), any())).thenReturn(userSummaryDto);

		// when: 로그인을 수행
		AuthLoginResponse response = authService.login(request, "127.0.0.1");

		// then: 토큰 응답과 Refresh 저장 동작을 검증
		assertThat(response.accessToken()).isEqualTo("access");
		assertThat(response.refreshToken()).isEqualTo("refresh");
		assertThat(response.passwordExpired()).isFalse();
		assertThat(response.user().name()).isEqualTo("홍길동");
		assertThat(response.user().email()).isEqualTo("user@example.com");
		
		verify(refreshTokenService).storeToken(eq(userDetails), eq("refresh"), eq("default"), eq(request.getDeviceInfo()), eq("127.0.0.1"));
		verify(loginAttemptService).validateNotLocked("user@example.com");
		verify(loginAttemptService).clearFailures("user@example.com");
	}

	@Test
	@DisplayName("리프레시 요청 시 새 토큰 쌍을 반환한다")
	void refresh_shouldReturnNewTokens() {
		// given: 리프레시 토큰 회전 결과와 활성 사용자 상태를 준비
		AuthService authService = newAuthService();

		TokenRefreshRequest request = new TokenRefreshRequest();
		request.setRefreshToken("old-token");

		RefreshTokenCache current = new RefreshTokenCache(
			"old-token",
			1L,
			"device-1",
			"ios",
			"127.0.0.1",
			1_735_000_000_000L,
			1_735_000_600_000L,
			1_735_000_000_000L
		);
		RefreshTokenCache rotated = new RefreshTokenCache(
			"new-token",
			1L,
			"device-1",
			"ios",
			"127.0.0.1",
			1L,
			2L,
			1L
		);
		when(refreshTokenService.loadValidToken("old-token")).thenReturn(current);
		when(refreshTokenService.rotateToken("old-token", "new-token")).thenReturn(rotated);
		when(jwtTokenService.createRefreshToken()).thenReturn("new-token");

		CustomUserDetails userDetails = mock(CustomUserDetails.class);
		when(userDetails.isEnabled()).thenReturn(true);
		when(userDetails.getUuid()).thenReturn(UUID.randomUUID());
		when(userDetailsService.loadUserById(1L)).thenReturn(userDetails);
		when(jwtTokenService.createAccessToken(userDetails, "device-1")).thenReturn("access");
		when(jwtTokenService.getAccessTokenExpirationSeconds()).thenReturn(1800L);
		when(jwtTokenService.getRefreshTokenExpirationSeconds()).thenReturn(604800L);

		// when: 리프레시를 수행
		TokenResponse response = authService.refresh(request);

		// then: 새 액세스/리프레시 토큰을 반환한다
		assertThat(response.accessToken()).isEqualTo("access");
		assertThat(response.refreshToken()).isEqualTo("new-token");
		assertThat(response.passwordExpired()).isFalse();
	}

	@Test
	@DisplayName("비활성 사용자면 리프레시가 실패한다")
	void refresh_shouldThrowWhenUserDisabled() {
		// given: 리프레시 회전 결과와 비활성 사용자 상태를 준비
		AuthService authService = newAuthService();

		TokenRefreshRequest request = new TokenRefreshRequest();
		request.setRefreshToken("old-token");

		RefreshTokenCache current = new RefreshTokenCache(
			"old-token",
			1L,
			"device-1",
			"ios",
			"127.0.0.1",
			1_735_000_000_000L,
			1_735_000_600_000L,
			1_735_000_000_000L
		);
		RefreshTokenCache rotated = new RefreshTokenCache(
			"new-token",
			1L,
			"device-1",
			"ios",
			"127.0.0.1",
			1L,
			2L,
			1L
		);
		when(refreshTokenService.loadValidToken("old-token")).thenReturn(current);
		when(refreshTokenService.rotateToken("old-token", "new-token")).thenReturn(rotated);
		when(jwtTokenService.createRefreshToken()).thenReturn("new-token");

		CustomUserDetails userDetails = mock(CustomUserDetails.class);
		when(userDetails.isEnabled()).thenReturn(false);
		when(userDetails.getUuid()).thenReturn(UUID.randomUUID());
		when(userDetailsService.loadUserById(1L)).thenReturn(userDetails);

		// when & then: 비활성 사용자는 예외가 발생한다
		assertThatThrownBy(() -> authService.refresh(request))
			.isInstanceOf(AuthException.class);
	}

	@Test
	@DisplayName("인증 실패 시 로그인 요청이 거절된다")
	void login_shouldThrowWhenAuthenticationFails() {
		// given: 인증 실패 상태를 준비
		AuthService authService = newAuthService();

		LoginRequest request = new LoginRequest();
		request.setEmail("user@example.com");
		request.setPassword("wrong-password");

		when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
			.thenThrow(new BadCredentialsException("Bad credentials"));

		// when & then: 인증 실패면 AuthException이 발생한다
		assertThatThrownBy(() -> authService.login(request, "127.0.0.1"))
			.isInstanceOf(AuthException.class);

		verifyNoInteractions(jwtTokenService, refreshTokenService);
		verify(loginAttemptService).validateNotLocked("user@example.com");
		verify(loginAttemptService).recordFailure("user@example.com");
		verify(loginAttemptService, never()).clearFailures(any());
	}

	@Test
	@DisplayName("로그인 잠금 상태인 계정은 인증 전에 429로 차단된다")
	void login_shouldRejectWhenLocked() {
		// given
		AuthService authService = newAuthService();
		LoginRequest request = new LoginRequest();
		request.setEmail("locked@example.com");
		request.setPassword("password");
		doThrow(new AuthException(AuthErrorCode.LOGIN_ATTEMPT_LIMITED))
			.when(loginAttemptService).validateNotLocked("locked@example.com");

		// when & then
		assertThatThrownBy(() -> authService.login(request, "127.0.0.1"))
			.isInstanceOf(AuthException.class)
			.hasMessage(AuthErrorCode.LOGIN_ATTEMPT_LIMITED.getMessage());
		verifyNoInteractions(authenticationManager, jwtTokenService, refreshTokenService);
	}

	@Test
	@DisplayName("5회째 로그인 실패 시 로그인 시도 제한 예외를 반환한다")
	void login_shouldThrowWhenFailureThresholdReached() {
		// given
		AuthService authService = newAuthService();
		LoginRequest request = new LoginRequest();
		request.setEmail("user@example.com");
		request.setPassword("wrong-password");

		when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
			.thenThrow(new BadCredentialsException("Bad credentials"));
		when(loginAttemptService.recordFailure("user@example.com")).thenReturn(true);

		// when & then
		assertThatThrownBy(() -> authService.login(request, "127.0.0.1"))
			.isInstanceOf(AuthException.class)
			.hasMessage(AuthErrorCode.LOGIN_ATTEMPT_LIMITED.getMessage());
		verify(loginAttemptService).validateNotLocked("user@example.com");
		verify(loginAttemptService).recordFailure("user@example.com");
		verify(loginAttemptService, never()).clearFailures(any());
	}

	@Test
	@DisplayName("이메일 미인증 사용자는 로그인 실패 메시지가 다르게 반환된다")
	void login_shouldThrowWhenEmailNotVerified() {
		// given: 이메일 미인증 상태를 준비
		AuthService authService = newAuthService();

		LoginRequest request = new LoginRequest();
		request.setEmail("user@example.com");
		request.setPassword("password");

		when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
			.thenThrow(new DisabledException("disabled"));

		// when & then: 이메일 인증 필요 예외가 발생한다
		assertThatThrownBy(() -> authService.login(request, "127.0.0.1"))
			.isInstanceOf(AuthException.class)
			.hasMessage(AuthErrorCode.EMAIL_VERIFICATION_REQUIRED.getMessage());
		verify(loginAttemptService).validateNotLocked("user@example.com");
		verify(loginAttemptService, never()).recordFailure(any());
		verify(loginAttemptService, never()).clearFailures(any());
	}

	@Test
	@DisplayName("리프레시 토큰이 유효하지 않으면 예외가 전파된다")
	void refresh_shouldThrowWhenRefreshTokenInvalid() {
		// given: 리프레시 토큰 검증 실패 상태를 준비
		AuthService authService = newAuthService();

		TokenRefreshRequest request = new TokenRefreshRequest();
		request.setRefreshToken("invalid-refresh");

		when(refreshTokenService.loadValidToken("invalid-refresh"))
			.thenThrow(new AuthException(AuthErrorCode.INVALID_REFRESH_TOKEN));

		// when & then: 예외가 그대로 전달된다
		assertThatThrownBy(() -> authService.refresh(request))
			.isInstanceOf(AuthException.class)
			.hasMessage(AuthErrorCode.INVALID_REFRESH_TOKEN.getMessage());
	}

	@Test
	@DisplayName("전체 로그아웃 시각 이전의 리프레시 토큰은 재발급이 거부된다")
	void refresh_shouldRejectTokenIssuedBeforeLogoutAll() {
		// given
		AuthService authService = newAuthService();

		TokenRefreshRequest request = new TokenRefreshRequest();
		request.setRefreshToken("old-token");

		RefreshTokenCache current = new RefreshTokenCache(
			"old-token",
			1L,
			"device-1",
			"ios",
			"127.0.0.1",
			1_735_000_000_000L,
			1_735_000_600_000L,
			1_735_000_000_000L
		);
		UUID uuid = UUID.randomUUID();
		CustomUserDetails userDetails = mock(CustomUserDetails.class);
		when(refreshTokenService.loadValidToken("old-token")).thenReturn(current);
		when(userDetailsService.loadUserById(1L)).thenReturn(userDetails);
		when(userDetails.getUuid()).thenReturn(uuid);
		when(accessTokenBlacklistService.getLogoutAllAt(uuid.toString())).thenReturn(Instant.ofEpochMilli(1_735_000_100_000L));

		// when & then
		assertThatThrownBy(() -> authService.refresh(request))
			.isInstanceOf(AuthException.class)
			.hasMessage(AuthErrorCode.INVALID_REFRESH_TOKEN.getMessage());
		verify(refreshTokenService).revokeToken("old-token");
	}

	@Test
	@DisplayName("비밀번호 변경 시 현재 비밀번호 검증 후 업데이트한다")
	void changePassword_shouldUpdatePassword() {
		// given
		AuthService authService = newAuthService();
		UserEntity user = mock(UserEntity.class);
		PasswordChangeRequest request = new PasswordChangeRequest("old", "new");

		when(user.getPassword()).thenReturn("encodedOld");
		when(user.getId()).thenReturn(1L);
		when(passwordEncoder.matches("old", "encodedOld")).thenReturn(true);
		when(passwordEncoder.matches("new", "encodedOld")).thenReturn(false);
		when(passwordEncoder.encode("new")).thenReturn("encodedNew");

		// when
		authService.changePassword(user, request);

		// then
		verify(userDomainService).updatePassword(1L, "encodedNew");
	}

	@Test
	@DisplayName("전체 로그아웃 시 사용자 refresh 토큰을 폐기하고 logout-all 기준 시각을 기록한다")
	void logoutAll_shouldRevokeRefreshTokensAndMarkLogoutAll() {
		// given
		AuthService authService = newAuthService();
		UserEntity user = mock(UserEntity.class);
		UUID uuid = UUID.randomUUID();
		when(user.getId()).thenReturn(1L);
		when(user.getUuid()).thenReturn(uuid);

		// when
		authService.logoutAll(user);

		// then
		verify(refreshTokenService).revokeAllByUserId(1L);
		verify(accessTokenBlacklistService).markLogoutAll(eq(uuid.toString()), any(Instant.class));
	}
}
