package com.aivle.project.auth.service;

import com.aivle.project.auth.dto.LoginRequest;
import com.aivle.project.auth.dto.PasswordChangeRequest;
import com.aivle.project.auth.dto.TokenRefreshRequest;
import com.aivle.project.auth.dto.TokenResponse;
import com.aivle.project.auth.exception.AuthErrorCode;
import com.aivle.project.auth.exception.AuthException;
import com.aivle.project.auth.token.JwtTokenService;
import com.aivle.project.auth.token.RefreshTokenCache;
import com.aivle.project.user.entity.UserEntity;
import com.aivle.project.user.security.CustomUserDetails;
import com.aivle.project.user.security.CustomUserDetailsService;
import com.aivle.project.user.service.UserDomainService;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
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
	private final CustomUserDetailsService userDetailsService;
	private final UserDomainService userDomainService;
	private final PasswordEncoder passwordEncoder;

	public TokenResponse login(LoginRequest request, String ipAddress) {
		Authentication authentication = authenticate(request.getEmail(), request.getPassword());
		CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
		String deviceId = normalizeDeviceId(request.getDeviceId());

		String accessToken = jwtTokenService.createAccessToken(userDetails, deviceId);
		String refreshToken = jwtTokenService.createRefreshToken();
		refreshTokenService.storeToken(userDetails, refreshToken, deviceId, request.getDeviceInfo(), ipAddress);

		boolean isPasswordExpired = userDetails.isPasswordExpired();

		return TokenResponse.of(
			accessToken,
			jwtTokenService.getAccessTokenExpirationSeconds(),
			refreshToken,
			jwtTokenService.getRefreshTokenExpirationSeconds(),
			isPasswordExpired
		);
	}

	public TokenResponse refresh(TokenRefreshRequest request) {
		String newRefreshToken = jwtTokenService.createRefreshToken();
		RefreshTokenCache rotated = refreshTokenService.rotateToken(request.getRefreshToken(), newRefreshToken);
		CustomUserDetails userDetails = (CustomUserDetails) userDetailsService.loadUserById(rotated.userId());
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

	public void logout(String refreshToken) {
		if (refreshToken != null && !refreshToken.isBlank()) {
			refreshTokenService.revokeToken(refreshToken);
		}
	}

	public void logoutAll(UserEntity user) {
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
}
