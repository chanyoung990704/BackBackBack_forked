package com.aivle.project.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TokenHashServiceTest {

	@Test
	@DisplayName("같은 토큰이라도 pepper가 다르면 해시가 달라진다")
	void hash_shouldDependOnPepper() {
		TokenHashService first = new TokenHashService(properties("dGVzdC1wZXBwZXI="));
		TokenHashService second = new TokenHashService(properties("YW5vdGhlci1wZXBwZXI="));

		String token = "sample-token";
		assertThat(first.hash(token)).isNotEqualTo(second.hash(token));
		assertThat(first.hash(token)).isNotEqualTo(first.legacyHash(token));
	}

	@Test
	@DisplayName("pepper 값이 없으면 서비스가 즉시 실패한다")
	void constructor_shouldFailWhenPepperMissing() {
		TokenHashProperties properties = new TokenHashProperties();
		assertThatThrownBy(() -> new TokenHashService(properties))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("APP_TOKEN_HASH_PEPPER_B64");
	}

	@Test
	@DisplayName("pepper Base64 형식이 아니면 서비스가 즉시 실패한다")
	void constructor_shouldFailWhenPepperInvalidBase64() {
		TokenHashProperties properties = new TokenHashProperties();
		properties.setPepperBase64("not-base64");

		assertThatThrownBy(() -> new TokenHashService(properties))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("Base64");
	}

	private TokenHashProperties properties(String pepperBase64) {
		TokenHashProperties properties = new TokenHashProperties();
		properties.setPepperBase64(pepperBase64);
		return properties;
	}
}
