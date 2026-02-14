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
}
