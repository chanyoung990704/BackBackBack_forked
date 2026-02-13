package com.aivle.project.auth.service;

import com.aivle.project.auth.dto.SignupRequest;
import com.aivle.project.auth.dto.SignupResponse;
import com.aivle.project.auth.exception.AuthErrorCode;
import com.aivle.project.auth.exception.AuthException;
import com.aivle.project.auth.mapper.AuthMapper;
import com.aivle.project.user.entity.RoleName;
import com.aivle.project.user.entity.UserEntity;
import com.aivle.project.user.entity.UserStatus;
import com.aivle.project.user.service.EmailVerificationService;
import com.aivle.project.user.service.UserDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원가입 처리 (이메일 인증 포함).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SignUpService {

	private final UserDomainService userDomainService;
	private final EmailVerificationService emailVerificationService;
	private final TurnstileService turnstileService;
	private final PasswordEncoder passwordEncoder;
	private final AuthMapper authMapper;

	@Value("${app.email.verification.skip:false}")
	private boolean skipEmailVerification;

	@Transactional
	public SignupResponse signup(SignupRequest request, String remoteIp) {
		log.info("Attempting signup for email: {}", request.getEmail());

		// 1. Turnstile 봇 검증
		if (!turnstileService.verifyTokenSync(request.getTurnstileToken(), remoteIp)) {
			log.warn("Signup rejected: Turnstile verification failed for email: {}", request.getEmail());
			throw new AuthException(AuthErrorCode.TURNSTILE_VERIFICATION_FAILED);
		}

		// 2. 이메일 중복 체크
		if (userDomainService.existsByEmail(request.getEmail())) {
			log.warn("Signup failed: email {} already exists", request.getEmail());
			throw new AuthException(AuthErrorCode.EMAIL_ALREADY_EXISTS);
		}

		String encodedPassword = passwordEncoder.encode(request.getPassword());
		UserEntity user;
		try {
			user = userDomainService.register(
				request.getEmail(),
				encodedPassword,
				request.getName(),
				null,
				RoleName.ROLE_USER
			);
		} catch (DataIntegrityViolationException ex) {
			log.warn("Signup failed due to DB unique conflict for email: {}", request.getEmail());
			throw new AuthException(AuthErrorCode.EMAIL_ALREADY_EXISTS);
		}

		if (skipEmailVerification) {
			userDomainService.activateUser(user.getId());
			user.setStatus(UserStatus.ACTIVE);
			log.info("Signup successful for email: {}, email verification skipped", request.getEmail());
			return authMapper.toSignupResponse(user, RoleName.ROLE_USER);
		}

		// 이메일 인증 토큰 생성 및 전송
		emailVerificationService.sendVerificationEmail(user, request.getEmail());

		log.info("Signup successful for email: {}, awaiting email verification", request.getEmail());
		return authMapper.toSignupResponse(user, RoleName.ROLE_USER);
	}
}
