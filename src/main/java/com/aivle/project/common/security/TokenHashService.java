package com.aivle.project.common.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 토큰 해시 계산 유틸리티.
 */
@Component
public class TokenHashService {

	public String hash(String token) {
		if (!StringUtils.hasText(token)) {
			throw new IllegalArgumentException("토큰 값이 비어 있습니다.");
		}
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] bytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
			return toHex(bytes);
		} catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", ex);
		}
	}

	private String toHex(byte[] bytes) {
		StringBuilder builder = new StringBuilder(bytes.length * 2);
		for (byte b : bytes) {
			builder.append(String.format("%02x", b));
		}
		return builder.toString();
	}
}
