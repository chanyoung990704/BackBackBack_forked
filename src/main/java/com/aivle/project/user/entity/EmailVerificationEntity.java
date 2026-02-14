package com.aivle.project.user.entity;

import com.aivle.project.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 이메일 인증 테이블.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "email_verifications")
public class EmailVerificationEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "email", nullable = false, length = 100)
    private String email;

    @Column(name = "token", length = 255)
    private String token;

    @Column(name = "token_hash", length = 64, unique = true)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private VerificationStatus status = VerificationStatus.PENDING;

    @Column(name = "expired_at", nullable = false)
    private LocalDateTime expiredAt;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    /**
     * 인증 토큰 생성.
     */
    public static EmailVerificationEntity create(UserEntity user, String email, String tokenHash, int expireMinutes) {
        EmailVerificationEntity verification = new EmailVerificationEntity();
        verification.user = user;
        verification.email = email;
        verification.tokenHash = tokenHash;
        verification.expiredAt = LocalDateTime.now().plusMinutes(expireMinutes);
        return verification;
    }

    public void migrateToHashed(String tokenHash) {
        this.tokenHash = tokenHash;
        this.token = null;
    }

    /**
     * 인증 완료.
     */
    public void verify() {
        this.status = VerificationStatus.VERIFIED;
        this.verifiedAt = LocalDateTime.now();
    }

    /**
     * 인증 만료.
     */
    public void expire() {
        this.status = VerificationStatus.EXPIRED;
    }

    /**
     * 인증 만료 여부 확인.
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiredAt);
    }

    /**
     * 이미 인증되었는지 확인.
     */
    public boolean isVerified() {
        return this.status == VerificationStatus.VERIFIED;
    }

    /**
     * 만료 시간 설정 (테스트용).
     */
    public void setExpiredAt(LocalDateTime expiredAt) {
        this.expiredAt = expiredAt;
    }
}
