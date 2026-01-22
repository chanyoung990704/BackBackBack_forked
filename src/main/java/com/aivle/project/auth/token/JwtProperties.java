package com.aivle.project.auth.token;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * JWT 설정 값 바인딩.
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

	@NotBlank
	private String issuer;

	private final AccessToken accessToken = new AccessToken();
	private final RefreshToken refreshToken = new RefreshToken();
	private final Keys keys = new Keys();
	private final Legacy legacy = new Legacy();

	@Getter
	@Setter
	public static class AccessToken {

		@Positive
		private long expiration;
	}

	@Getter
	@Setter
	public static class RefreshToken {

		@Positive
		private long expiration;
	}

	@Getter
	@Setter
	public static class Keys {

		@NotBlank
		private String privateKeyPath;

		@NotBlank
		private String publicKeyPath;

		@NotBlank
		private String currentKid;
	}

	@Getter
	@Setter
	public static class Legacy {

		private boolean rolePrefixSupportEnabled = true;

		@PositiveOrZero
		private long rolePrefixAcceptUntilEpoch = 0;
	}
}
