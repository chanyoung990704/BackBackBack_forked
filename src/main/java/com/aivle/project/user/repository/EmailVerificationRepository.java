package com.aivle.project.user.repository;

import com.aivle.project.user.entity.EmailVerificationEntity;
import com.aivle.project.user.entity.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailVerificationRepository extends JpaRepository<EmailVerificationEntity, Long> {

    /**
     * 토큰으로 인증 정보 조회.
     */
    Optional<EmailVerificationEntity> findByToken(String token);

    /**
     * 토큰 해시로 인증 정보 조회.
     */
    Optional<EmailVerificationEntity> findByTokenHash(String tokenHash);

    /**
     * 이메일로 가장 최근 인증 정보 조회.
     */
    @Query("SELECT ev FROM EmailVerificationEntity ev " +
           "WHERE ev.email = :email AND ev.status = :status " +
           "ORDER BY ev.createdAt DESC LIMIT 1")
    Optional<EmailVerificationEntity> findLatestByEmailAndStatus(
            @Param("email") String email,
            @Param("status") VerificationStatus status
    );

    /**
     * 이메일로 인증 대기 중인 인증 정보 존재 여부 확인.
     */
    boolean existsByEmailAndStatus(String email, VerificationStatus status);
}
