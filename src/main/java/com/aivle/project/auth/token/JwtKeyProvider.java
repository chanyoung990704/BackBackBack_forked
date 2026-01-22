package com.aivle.project.auth.token;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * RSA 키 파일을 로드하는 유틸리티.
 */
@Component
@RequiredArgsConstructor
public class JwtKeyProvider {

	private final JwtProperties jwtProperties;

	public RSAPrivateKey loadPrivateKey() {
		Path path = Path.of(jwtProperties.getKeys().getPrivateKeyPath());
		byte[] keyBytes = readPemBytes(path, "PRIVATE KEY");
		return parsePrivateKey(keyBytes);
	}

	public RSAPublicKey loadPublicKey() {
		Path path = Path.of(jwtProperties.getKeys().getPublicKeyPath());
		byte[] keyBytes = readPemBytes(path, "PUBLIC KEY");
		return parsePublicKey(keyBytes);
	}

	private byte[] readPemBytes(Path path, String keyType) {
		try {
			String pem = Files.readString(path);
			String normalized = pem
				.replace("-----BEGIN " + keyType + "-----", "")
				.replace("-----END " + keyType + "-----", "")
				.replaceAll("\\s", "");
			return Base64.getDecoder().decode(normalized);
		} catch (IOException ex) {
			throw new IllegalStateException("키 파일을 읽을 수 없습니다: " + path, ex);
		}
	}

	private RSAPrivateKey parsePrivateKey(byte[] keyBytes) {
		try {
			KeyFactory factory = KeyFactory.getInstance("RSA");
			return (RSAPrivateKey) factory.generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
		} catch (Exception ex) {
			throw new IllegalStateException("Private Key 파싱에 실패했습니다", ex);
		}
	}

	private RSAPublicKey parsePublicKey(byte[] keyBytes) {
		try {
			KeyFactory factory = KeyFactory.getInstance("RSA");
			return (RSAPublicKey) factory.generatePublic(new X509EncodedKeySpec(keyBytes));
		} catch (Exception ex) {
			throw new IllegalStateException("Public Key 파싱에 실패했습니다", ex);
		}
	}
}
