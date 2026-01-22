package com.aivle.project.auth.service;

import com.aivle.project.auth.dto.LoginRequest;
import com.aivle.project.auth.dto.TokenRefreshRequest;
import com.aivle.project.auth.dto.TokenResponse;
import com.aivle.project.auth.exception.AuthErrorCode;
import com.aivle.project.auth.exception.AuthException;
import com.aivle.project.auth.token.JwtTokenService;
import com.aivle.project.auth.token.RefreshTokenCache;
import com.aivle.project.user.security.CustomUserDetails;
import com.aivle.project.user.security.CustomUserDetailsService;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

/**
 * 로그인 및 토큰 재발급 처리.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

	private final AuthenticationManager authenticationManager;
	private final JwtTokenService jwtTokenService;
	private final RefreshTokenService refreshTokenService;
	private final CustomUserDetailsService userDetailsService;
	private final AccessTokenBlacklistService accessTokenBlacklistService;

	public TokenResponse login(LoginRequest request, String ipAddress) {
		log.info("Attempting login for user: {}, IP: {}", request.getEmail(), ipAddress);
		Authentication authentication = authenticate(request.getEmail(), request.getPassword());
		CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
		String deviceId = normalizeDeviceId(request.getDeviceId());

		String accessToken = jwtTokenService.createAccessToken(userDetails, deviceId);
		String refreshToken = jwtTokenService.createRefreshToken();
		refreshTokenService.storeToken(userDetails, refreshToken, deviceId, request.getDeviceInfo(), ipAddress);

		log.info("Login successful for user: {}, deviceId: {}", request.getEmail(), deviceId);
		return TokenResponse.of(
			accessToken,
			jwtTokenService.getAccessTokenExpirationSeconds(),
			refreshToken,
			jwtTokenService.getRefreshTokenExpirationSeconds()
		);
	}

	public TokenResponse refresh(TokenRefreshRequest request) {
		log.info("Attempting token refresh");
		String newRefreshToken = jwtTokenService.createRefreshToken();
		RefreshTokenCache rotated = refreshTokenService.rotateToken(request.getRefreshToken(), newRefreshToken);
		CustomUserDetails userDetails = (CustomUserDetails) loadUser(rotated.email());
		if (!userDetails.isEnabled()) {
			log.warn("Refresh failed: user {} is disabled", rotated.email());
			throw new AuthException(AuthErrorCode.INVALID_REFRESH_TOKEN);
		}

		String accessToken = jwtTokenService.createAccessToken(userDetails, rotated.deviceId());
		log.info("Token refresh successful for user: {}", rotated.email());
		return TokenResponse.of(
			accessToken,
			jwtTokenService.getAccessTokenExpirationSeconds(),
			newRefreshToken,
			jwtTokenService.getRefreshTokenExpirationSeconds()
		);
	}

	public void logout(Jwt jwt) {
		if (jwt == null) {
			throw new AuthException(AuthErrorCode.INVALID_ACCESS_TOKEN);
		}
		String userId = resolveUserId(jwt);
		String deviceId = normalizeDeviceId(jwt.getClaimAsString("deviceId"));
		String jti = resolveTokenId(jwt);
		Instant expiresAt = resolveExpiresAt(jwt);

		log.info("Logout requested for user: {}, deviceId: {}", userId, deviceId);
		accessTokenBlacklistService.blacklist(jti, expiresAt);
		refreshTokenService.revokeByUserIdAndDeviceId(userId, deviceId);
	}

	public void logoutAll(Jwt jwt) {
		if (jwt == null) {
			throw new AuthException(AuthErrorCode.INVALID_ACCESS_TOKEN);
		}
		String userId = resolveUserId(jwt);
		String jti = resolveTokenId(jwt);
		Instant expiresAt = resolveExpiresAt(jwt);

		log.info("Logout-all requested for user: {}", userId);
		accessTokenBlacklistService.blacklist(jti, expiresAt);
		accessTokenBlacklistService.markLogoutAll(userId, Instant.now());
		refreshTokenService.revokeByUserId(userId);
	}

	private Authentication authenticate(String email, String password) {
		try {
			return authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));
		} catch (AuthenticationException ex) {
			throw new AuthException(AuthErrorCode.INVALID_CREDENTIALS);
		}
	}

	private UserDetails loadUser(String email) {
		return userDetailsService.loadUserByUsername(email);
	}

	private String normalizeDeviceId(String deviceId) {
		if (deviceId == null || deviceId.isBlank()) {
			return "default";
		}
		return deviceId;
	}

	private String resolveUserId(Jwt jwt) {
		String subject = jwt.getSubject();
		if (subject == null || subject.isBlank()) {
			throw new AuthException(AuthErrorCode.INVALID_ACCESS_TOKEN);
		}
		return subject;
	}

	private String resolveTokenId(Jwt jwt) {
		String jti = jwt.getId();
		if (jti == null || jti.isBlank()) {
			throw new AuthException(AuthErrorCode.INVALID_ACCESS_TOKEN);
		}
		return jti;
	}

	private Instant resolveExpiresAt(Jwt jwt) {
		Instant expiresAt = jwt.getExpiresAt();
		if (expiresAt != null) {
			return expiresAt;
		}
		return Instant.now().plusSeconds(jwtTokenService.getAccessTokenExpirationSeconds());
	}
}
