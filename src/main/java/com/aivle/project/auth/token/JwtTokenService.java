package com.aivle.project.auth.token;

import com.aivle.project.user.security.CustomUserDetails;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/**
 * JWT 발급 유틸리티.
 */
@Service
@RequiredArgsConstructor
public class JwtTokenService {

	private static final int REFRESH_TOKEN_BYTES = 32;

	private final JwtEncoder jwtEncoder;
	private final JwtProperties jwtProperties;
	private final SecureRandom secureRandom = new SecureRandom();

	public String createAccessToken(CustomUserDetails userDetails, String deviceId) {
		Instant now = Instant.now();
		Instant expiresAt = now.plusSeconds(jwtProperties.getAccessToken().getExpiration());

		List<String> roles = userDetails.getAuthorities().stream()
			.map(authority -> authority.getAuthority())
			.map(this::stripRolePrefix)
			.toList();

		JwtClaimsSet claims = JwtClaimsSet.builder()
			.issuer(jwtProperties.getIssuer())
			.subject(userDetails.getUuid().toString())
			.issuedAt(now)
			.expiresAt(expiresAt)
			.id("at-" + UUID.randomUUID())
			.claim("email", userDetails.getUsername())
			.claim("roles", roles)
			.claim("deviceId", deviceId)
			.build();

		JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256)
			.keyId(jwtProperties.getKeys().getCurrentKid())
			.build();

		return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
	}

	public String createRefreshToken() {
		byte[] bytes = new byte[REFRESH_TOKEN_BYTES];
		secureRandom.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	public long getAccessTokenExpirationSeconds() {
		return jwtProperties.getAccessToken().getExpiration();
	}

	public long getRefreshTokenExpirationSeconds() {
		return jwtProperties.getRefreshToken().getExpiration();
	}

	private String stripRolePrefix(String authority) {
		if (authority == null) {
			return "";
		}
		if (authority.startsWith("ROLE_")) {
			return authority.substring("ROLE_".length());
		}
		return authority;
	}
}
