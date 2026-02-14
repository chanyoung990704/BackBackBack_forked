package com.aivle.project.common.security;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Objects;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 토큰 해시 계산 유틸리티.
 */
@Component
public class TokenHashService {

	private static final String HMAC_ALGORITHM = "HmacSHA256";
	private final byte[] pepper;

	public TokenHashService(TokenHashProperties properties) {
		String pepperBase64 = properties.getPepperBase64();
		if (!StringUtils.hasText(pepperBase64)) {
			throw new IllegalStateException("APP_TOKEN_HASH_PEPPER_B64가 설정되지 않았습니다.");
		}
		try {
			this.pepper = Base64.getDecoder().decode(pepperBase64.trim());
		} catch (IllegalArgumentException ex) {
			throw new IllegalStateException("APP_TOKEN_HASH_PEPPER_B64는 유효한 Base64 문자열이어야 합니다.", ex);
		}
		if (pepper.length == 0) {
			throw new IllegalStateException("APP_TOKEN_HASH_PEPPER_B64가 비어 있습니다.");
		}
	}

	public String hash(String token) {
		if (!StringUtils.hasText(token)) {
			throw new IllegalArgumentException("토큰 값이 비어 있습니다.");
		}
		try {
			Mac mac = Mac.getInstance(HMAC_ALGORITHM);
			mac.init(new SecretKeySpec(pepper, HMAC_ALGORITHM));
			byte[] bytes = mac.doFinal(token.getBytes(StandardCharsets.UTF_8));
			return toHex(bytes);
		} catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("HmacSHA256 알고리즘을 사용할 수 없습니다.", ex);
		} catch (InvalidKeyException ex) {
			throw new IllegalStateException("토큰 해시 pepper 키가 유효하지 않습니다.", ex);
		}
	}

	/**
	 * 레거시 SHA-256 해시 계산.
	 */
	public String legacyHash(String token) {
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

	public boolean isLegacyHash(String token, String tokenHash) {
		if (!StringUtils.hasText(tokenHash)) {
			return false;
		}
		return Objects.equals(legacyHash(token), tokenHash);
	}

	private String toHex(byte[] bytes) {
		StringBuilder builder = new StringBuilder(bytes.length * 2);
		for (byte b : bytes) {
			builder.append(String.format("%02x", b));
		}
		return builder.toString();
	}
}
