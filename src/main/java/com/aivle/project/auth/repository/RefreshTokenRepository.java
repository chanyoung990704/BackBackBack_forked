package com.aivle.project.auth.repository;

import com.aivle.project.auth.entity.RefreshTokenEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Refresh Token 저장소.
 */
public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, Long> {

	Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

	Optional<RefreshTokenEntity> findByTokenValue(String tokenValue);

	List<RefreshTokenEntity> findAllByUserIdAndRevokedFalse(Long userId);

	@org.springframework.data.jpa.repository.Modifying
	@org.springframework.data.jpa.repository.Query("UPDATE RefreshTokenEntity r SET r.revoked = true WHERE r.tokenHash = :tokenHash AND r.revoked = false")
	int revokeTokenIfValid(@org.springframework.data.repository.query.Param("tokenHash") String tokenHash);
}
