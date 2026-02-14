package com.aivle.project.user.service;

import com.aivle.project.auth.service.VerificationResendRateLimiter;
import com.aivle.project.common.security.TokenHashService;
import com.aivle.project.user.entity.EmailVerificationEntity;
import com.aivle.project.user.entity.UserEntity;
import com.aivle.project.user.entity.UserStatus;
import com.aivle.project.user.entity.VerificationStatus;
import com.aivle.project.user.repository.EmailVerificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

/**
 * 이메일 인증 서비스.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final EmailVerificationRepository emailVerificationRepository;
    private final EmailService emailService;
    private final UserDomainService userDomainService;
    private final VerificationResendRateLimiter verificationResendRateLimiter;
    private final TokenHashService tokenHashService;

    @Value("${app.email.verification.expire-minutes:30}")
    private int expireMinutes;

    @Value("${app.email.verification.token-length:32}")
    private int tokenLength;

    /**
     * 이메일 인증 토큰 생성 및 전송.
     */
    @Transactional
    public void sendVerificationEmail(UserEntity user, String email) {
        // 기존 인증 대기 중인 토큰이 있다면 만료 처리
        emailVerificationRepository.findLatestByEmailAndStatus(email, VerificationStatus.PENDING)
                .ifPresent(existing -> existing.expire());

        // 새로운 인증 토큰 생성
        String token = generateSecureToken();
        String tokenHash = tokenHashService.hash(token);
        EmailVerificationEntity verification = EmailVerificationEntity.create(user, email, tokenHash, expireMinutes);
        emailVerificationRepository.save(verification);

        // 이메일 전송
        emailService.sendVerificationEmail(email, token);

        log.info("이메일 인증 토큰 생성 완료: {}", email);
    }

    /**
     * 이메일 인증 처리.
     */
    @Transactional
    public void verifyEmail(String token) {
        String tokenHash = tokenHashService.hash(token);
        String legacyTokenHash = tokenHashService.legacyHash(token);
        EmailVerificationEntity verification = emailVerificationRepository.findByTokenHash(tokenHash)
                .or(() -> emailVerificationRepository.findByTokenHash(legacyTokenHash))
                .or(() -> emailVerificationRepository.findByToken(token))
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 인증 토큰입니다."));
        if (verification.getTokenHash() == null || tokenHashService.isLegacyHash(token, verification.getTokenHash())) {
            verification.migrateToHashed(tokenHash);
        }

        // 만료 여부 확인
        if (verification.isExpired()) {
            verification.expire();
            throw new IllegalArgumentException("인증 토큰이 만료되었습니다.");
        }

        // 이미 인증되었는지 확인
        if (verification.isVerified()) {
            throw new IllegalArgumentException("이미 인증된 이메일입니다.");
        }

        // 인증 완료
        verification.verify();

        // 사용자 상태 업데이트 (PENDING → ACTIVE)
        userDomainService.activateUser(verification.getUser().getId());

        log.info("이메일 인증 완료: {}", verification.getEmail());
    }

    /**
     * 인증 토큰 재전송.
     */
    @Transactional
    public void resendVerificationEmail(Long userId) {
        resendVerificationEmail(userId, null);
    }

    /**
     * 인증 토큰 재전송.
     */
    @Transactional
    public void resendVerificationEmail(Long userId, String clientIp) {
        verificationResendRateLimiter.checkLimit(userId, clientIp);
        UserEntity user = userDomainService.getUserById(userId);

        // 이미 인증된 사용자는 재전송 불가
        if (user.getStatus() == UserStatus.ACTIVE) {
            throw new IllegalStateException("이미 인증된 사용자입니다.");
        }

        sendVerificationEmail(user, user.getEmail());
    }

    /**
     * 보안 토큰 생성.
     */
    private String generateSecureToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[tokenLength];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * 만료된 인증 정보 정리 (스케줄러용).
     */
    @Transactional
    public void cleanupExpiredVerifications() {
        LocalDateTime now = LocalDateTime.now();
        emailVerificationRepository.findAll().stream()
                .filter(verification -> verification.getStatus() == VerificationStatus.PENDING)
                .filter(verification -> verification.getExpiredAt().isBefore(now))
                .forEach(verification -> {
                    verification.expire();
                    log.debug("만료된 인증 토큰 정리: {}", verification.getEmail());
                });
    }
}
