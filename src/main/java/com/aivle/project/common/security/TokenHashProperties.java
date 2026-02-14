package com.aivle.project.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 토큰 해시 pepper 설정.
 */
@Component
@ConfigurationProperties(prefix = "app.security.token-hash")
public class TokenHashProperties {

	private String pepperBase64;

	public String getPepperBase64() {
		return pepperBase64;
	}

	public void setPepperBase64(String pepperBase64) {
		this.pepperBase64 = pepperBase64;
	}
}
