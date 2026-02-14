package com.aivle.project.user.service;

import com.aivle.project.common.config.TestSecurityConfig;
import com.aivle.project.auth.service.VerificationResendRateLimiter;
import com.aivle.project.user.entity.EmailVerificationEntity;
import com.aivle.project.user.entity.UserEntity;
import com.aivle.project.user.entity.UserStatus;
import com.aivle.project.user.entity.VerificationStatus;
import com.aivle.project.user.repository.EmailVerificationRepository;
import com.aivle.project.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import(TestSecurityConfig.class)
@DisplayName("이메일 인증 서비스 테스트")
class EmailVerificationServiceTest {

    @Autowired
    private EmailVerificationService emailVerificationService;

    @Autowired
    private EmailVerificationRepository emailVerificationRepository;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private EmailService emailService;

    @MockBean
    private VerificationResendRateLimiter verificationResendRateLimiter;

    private UserEntity testUser;

    @BeforeEach
    void setUp() {
        // 테스트 사용자 생성
        testUser = UserEntity.create(
            "test@example.com",
            "encodedPassword",
            "Test User",
            "010-1234-5678",
            UserStatus.PENDING
        );
        userRepository.save(testUser);
    }

    @Test
    @DisplayName("이메일 인증 토큰 생성 및 전송")
    void sendVerificationEmail() {
        // When
        emailVerificationService.sendVerificationEmail(testUser, "test@example.com");

        // Then
        verify(emailService, times(1)).sendVerificationEmail(eq("test@example.com"), anyString());
        assertThat(emailVerificationRepository.findAll()).hasSize(1);

        EmailVerificationEntity verification = emailVerificationRepository.findAll().get(0);
        assertThat(verification.getEmail()).isEqualTo("test@example.com");
        assertThat(verification.getStatus()).isEqualTo(VerificationStatus.PENDING);
        assertThat(verification.getToken()).isNull();
        assertThat(verification.getTokenHash()).isNotBlank();
        assertThat(verification.getExpiredAt()).isAfter(LocalDateTime.now());
    }

    @Test
    @DisplayName("이메일 인증 성공")
    void verifyEmail_Success() {
        // Given
        emailVerificationService.sendVerificationEmail(testUser, "test@example.com");
        Long verificationId = emailVerificationRepository.findAll().get(0).getId();
        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendVerificationEmail(eq("test@example.com"), tokenCaptor.capture());
        String token = tokenCaptor.getValue();

        // When
        emailVerificationService.verifyEmail(token);

        // Then
        EmailVerificationEntity updatedVerification = emailVerificationRepository.findById(verificationId).orElseThrow();
        assertThat(updatedVerification.getStatus()).isEqualTo(VerificationStatus.VERIFIED);
        assertThat(updatedVerification.getVerifiedAt()).isNotNull();

        UserEntity updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(updatedUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    @DisplayName("만료된 토큰으로 인증 시도")
    void verifyEmail_ExpiredToken() {
        // Given
        emailVerificationService.sendVerificationEmail(testUser, "test@example.com");
        EmailVerificationEntity verification = emailVerificationRepository.findAll().get(0);
        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendVerificationEmail(eq("test@example.com"), tokenCaptor.capture());
        String token = tokenCaptor.getValue();

        // 만료 시간을 과거로 설정
        verification.setExpiredAt(LocalDateTime.now().minusMinutes(1));
        emailVerificationRepository.save(verification);

        // When & Then
        assertThatThrownBy(() -> emailVerificationService.verifyEmail(token))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("만료되었습니다");

        EmailVerificationEntity updatedVerification = emailVerificationRepository.findById(verification.getId()).orElseThrow();
        assertThat(updatedVerification.getStatus()).isEqualTo(VerificationStatus.EXPIRED);
    }

    @Test
    @DisplayName("유효하지 않은 토큰으로 인증 시도")
    void verifyEmail_InvalidToken() {
        // When & Then
        assertThatThrownBy(() -> emailVerificationService.verifyEmail("invalid-token"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("유효하지 않은");
    }

    @Test
    @DisplayName("인증 재전송")
    void resendVerificationEmail() {
        // Given
        emailVerificationService.sendVerificationEmail(testUser, "test@example.com");

        // When
        emailVerificationService.resendVerificationEmail(testUser.getId(), "127.0.0.1");

        // Then
        verify(verificationResendRateLimiter).checkLimit(testUser.getId(), "127.0.0.1");
        verify(emailService, times(2)).sendVerificationEmail(eq("test@example.com"), anyString());
        assertThat(emailVerificationRepository.findAll()).hasSize(2);
    }

    @Test
    @DisplayName("만료된 인증 정보 정리")
    void cleanupExpiredVerifications() {
        // Given
        emailVerificationService.sendVerificationEmail(testUser, "test@example.com");
        EmailVerificationEntity verification = emailVerificationRepository.findAll().get(0);

        // 만료 시간을 과거로 설정
        verification.setExpiredAt(LocalDateTime.now().minusMinutes(1));
        emailVerificationRepository.save(verification);

        // When
        emailVerificationService.cleanupExpiredVerifications();

        // Then
        EmailVerificationEntity updatedVerification = emailVerificationRepository.findById(verification.getId()).orElseThrow();
        assertThat(updatedVerification.getStatus()).isEqualTo(VerificationStatus.EXPIRED);
    }
}
