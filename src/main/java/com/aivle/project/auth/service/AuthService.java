package com.aivle.project.auth.service;

import com.aivle.project.auth.dto.AuthLoginResponse;
import com.aivle.project.auth.dto.LoginRequest;
import com.aivle.project.auth.dto.PasswordChangeRequest;
import com.aivle.project.auth.dto.TokenRefreshRequest;
import com.aivle.project.auth.dto.TokenResponse;
import com.aivle.project.auth.exception.AuthErrorCode;
import com.aivle.project.auth.exception.AuthException;
import com.aivle.project.auth.token.JwtTokenService;
import com.aivle.project.auth.token.RefreshTokenCache;
import com.aivle.project.auth.user.dto.UserSummaryDto;
import com.aivle.project.auth.user.entity.RoleName;
import com.aivle.project.auth.user.entity.UserEntity;
import com.aivle.project.auth.user.security.CustomUserDetails;
import com.aivle.project.auth.user.security.CustomUserDetailsService;
import com.aivle.project.auth.user.service.UserDomainService;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 로그인 및 토큰 재발급 처리.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

	private final AuthenticationManager authenticationManager;
	private final JwtTokenService jwtTokenService;
	private final RefreshTokenService refreshTokenService;
	private final AccessTokenBlacklistService accessTokenBlacklistService;
	private final LoginAttemptService loginAttemptService;
	private final CustomUserDetailsService userDetailsService;
	private final UserDomainService userDomainService;
	private final PasswordEncoder passwordEncoder;
	private final com.aivle.project.auth.user.mapper.UserMapper userMapper;

	public AuthLoginResponse login(LoginRequest request, String ipAddress) {
		String email = normalizeEmail(request.getEmail());
		loginAttemptService.validateNotLocked(email);
		Authentication authentication;
		try {
			authentication = authenticate(request.getEmail(), request.getPassword());
		} catch (AuthException ex) {
			if (ex.getErrorCode() == AuthErrorCode.INVALID_CREDENTIALS) {
				boolean locked = loginAttemptService.recordFailure(email);
				if (locked) {
					throw new AuthException(AuthErrorCode.LOGIN_ATTEMPT_LIMITED);
				}
			}
			throw ex;
		}

		loginAttemptService.clearFailures(email);
		CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
		String deviceId = normalizeDeviceId(request.getDeviceId());

		String accessToken = jwtTokenService.createAccessToken(userDetails, deviceId);
		String refreshToken = jwtTokenService.createRefreshToken();
		refreshTokenService.storeToken(userDetails, refreshToken, deviceId, request.getDeviceInfo(), ipAddress);

		boolean isPasswordExpired = userDetails.isPasswordExpired();

		TokenResponse tokenResponse = TokenResponse.of(
			accessToken,
			jwtTokenService.getAccessTokenExpirationSeconds(),
			refreshToken,
			jwtTokenService.getRefreshTokenExpirationSeconds(),
			isPasswordExpired
		);

		RoleName role = userDetails.getAuthorities().stream()
			.map(GrantedAuthority::getAuthority)
			.filter(a -> a.startsWith("ROLE_"))
			.map(RoleName::valueOf)
			.findFirst()
			.orElse(RoleName.ROLE_USER);

		UserSummaryDto userSummary = userMapper.toSummaryDto(userDetails, role);

		return AuthLoginResponse.of(tokenResponse, userSummary);
	}

	public TokenResponse refresh(TokenRefreshRequest request) {
		RefreshTokenCache currentToken = refreshTokenService.loadValidToken(request.getRefreshToken());
		CustomUserDetails userDetails = (CustomUserDetails) userDetailsService.loadUserById(currentToken.userId());
		validateRefreshTokenAfterLogoutAll(request.getRefreshToken(), userDetails, currentToken);

		String newRefreshToken = jwtTokenService.createRefreshToken();
		RefreshTokenCache rotated = refreshTokenService.rotateToken(request.getRefreshToken(), newRefreshToken);
		if (!userDetails.isEnabled()) {
			throw new AuthException(AuthErrorCode.INVALID_REFRESH_TOKEN);
		}

		String accessToken = jwtTokenService.createAccessToken(userDetails, rotated.deviceId());
		return TokenResponse.of(
			accessToken,
			jwtTokenService.getAccessTokenExpirationSeconds(),
			newRefreshToken,
			jwtTokenService.getRefreshTokenExpirationSeconds(),
			userDetails.isPasswordExpired()
		);
	}

	private void validateRefreshTokenAfterLogoutAll(
		String refreshToken,
		CustomUserDetails userDetails,
		RefreshTokenCache refreshTokenCache
	) {
		Instant logoutAllAt = accessTokenBlacklistService.getLogoutAllAt(userDetails.getUuid().toString());
		if (logoutAllAt == null) {
			return;
		}

		Instant tokenUsedAt = Instant.ofEpochMilli(normalizeEpochMillis(refreshTokenCache.lastUsedAt()));
		if (!tokenUsedAt.isAfter(logoutAllAt)) {
			try {
				refreshTokenService.revokeToken(refreshToken);
			} catch (AuthException ignored) {
				// 이미 무효화된 토큰은 추가 처리하지 않는다.
			}
			throw new AuthException(AuthErrorCode.INVALID_REFRESH_TOKEN);
		}
	}

	public void logout(String refreshToken, Jwt jwt) {
		if (refreshToken != null && !refreshToken.isBlank()) {
			refreshTokenService.revokeToken(refreshToken);
		}
		if (jwt != null) {
			accessTokenBlacklistService.blacklist(jwt.getId(), jwt.getExpiresAt());
		}
	}

	public void logoutAll(UserEntity user) {
		refreshTokenService.revokeAllByUserId(user.getId());
		accessTokenBlacklistService.markLogoutAll(user.getUuid().toString(), Instant.now());
	}

	@Transactional
	public void changePassword(UserEntity user, PasswordChangeRequest request) {
		// 현재 비밀번호 검증
		if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
			throw new AuthException(AuthErrorCode.INVALID_CREDENTIALS);
		}

		// 새 비밀번호가 기존 비밀번호와 같은지 검증
		if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
			throw new IllegalArgumentException("새 비밀번호는 기존 비밀번호와 다르게 설정해야 합니다.");
		}

		// 비밀번호 변경
		userDomainService.updatePassword(user.getId(), passwordEncoder.encode(request.getNewPassword()));
	}

	private Authentication authenticate(String email, String password) {
		try {
			return authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));
		} catch (DisabledException ex) {
			throw new AuthException(AuthErrorCode.EMAIL_VERIFICATION_REQUIRED);
		} catch (AuthenticationException ex) {
			throw new AuthException(AuthErrorCode.INVALID_CREDENTIALS);
		}
	}

	private String normalizeDeviceId(String deviceId) {
		if (deviceId == null || deviceId.isBlank()) {
			return "default";
		}
		return deviceId;
	}

	private long normalizeEpochMillis(long epochValue) {
		// 레거시(초 단위) 캐시와 신규(밀리초 단위) 캐시를 모두 허용한다.
		if (epochValue > 0 && epochValue < 10_000_000_000L) {
			return epochValue * 1000;
		}
		return epochValue;
	}

	private String normalizeEmail(String email) {
		if (email == null) {
			return "";
		}
		return email.trim();
	}
}
