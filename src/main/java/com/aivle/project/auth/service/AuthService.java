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
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

/**
 * 로그인 및 토큰 재발급 처리.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

	private final AuthenticationManager authenticationManager;
	private final JwtTokenService jwtTokenService;
	private final RefreshTokenService refreshTokenService;
	private final CustomUserDetailsService userDetailsService;

	public TokenResponse login(LoginRequest request, String ipAddress) {
		Authentication authentication = authenticate(request.getEmail(), request.getPassword());
		CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
		String deviceId = normalizeDeviceId(request.getDeviceId());

		String accessToken = jwtTokenService.createAccessToken(userDetails, deviceId);
		String refreshToken = jwtTokenService.createRefreshToken();
		refreshTokenService.storeToken(userDetails, refreshToken, deviceId, request.getDeviceInfo(), ipAddress);

		return TokenResponse.of(
			accessToken,
			jwtTokenService.getAccessTokenExpirationSeconds(),
			refreshToken,
			jwtTokenService.getRefreshTokenExpirationSeconds()
		);
	}

	public TokenResponse refresh(TokenRefreshRequest request) {
		String newRefreshToken = jwtTokenService.createRefreshToken();
		RefreshTokenCache rotated = refreshTokenService.rotateToken(request.getRefreshToken(), newRefreshToken);
		CustomUserDetails userDetails = (CustomUserDetails) loadUser(rotated.email());
		if (!userDetails.isEnabled()) {
			throw new AuthException(AuthErrorCode.INVALID_REFRESH_TOKEN);
		}

		String accessToken = jwtTokenService.createAccessToken(userDetails, rotated.deviceId());
		return TokenResponse.of(
			accessToken,
			jwtTokenService.getAccessTokenExpirationSeconds(),
			newRefreshToken,
			jwtTokenService.getRefreshTokenExpirationSeconds()
		);
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
}
