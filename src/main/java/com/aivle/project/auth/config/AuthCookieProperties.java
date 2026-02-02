package com.aivle.project.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 리프레시 토큰 쿠키 정책 설정.
 */
@Component
@ConfigurationProperties(prefix = "app.auth.cookie")
public class AuthCookieProperties {

	private boolean secure = true;
	private String sameSite = "Strict";

	public boolean isSecure() {
		return secure;
	}

	public void setSecure(boolean secure) {
		this.secure = secure;
	}

	public String getSameSite() {
		return sameSite;
	}

	public void setSameSite(String sameSite) {
		this.sameSite = sameSite;
	}
}
