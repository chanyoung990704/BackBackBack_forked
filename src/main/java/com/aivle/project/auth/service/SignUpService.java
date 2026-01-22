package com.aivle.project.auth.service;

import com.aivle.project.auth.dto.SignupRequest;
import com.aivle.project.auth.dto.SignupResponse;
import com.aivle.project.auth.exception.AuthErrorCode;
import com.aivle.project.auth.exception.AuthException;
import com.aivle.project.auth.mapper.AuthMapper;
import com.aivle.project.user.entity.RoleName;
import com.aivle.project.user.entity.UserEntity;
import com.aivle.project.user.service.UserDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원가입 처리.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SignUpService {

	private final UserDomainService userDomainService;
	private final PasswordEncoder passwordEncoder;
	private final AuthMapper authMapper;

	@Transactional
	public SignupResponse signup(SignupRequest request) {
		log.info("Attempting signup for email: {}", request.getEmail());
		if (userDomainService.existsByEmail(request.getEmail())) {
			log.warn("Signup failed: email {} already exists", request.getEmail());
			throw new AuthException(AuthErrorCode.EMAIL_ALREADY_EXISTS);
		}

		String encodedPassword = passwordEncoder.encode(request.getPassword());
		UserEntity user = userDomainService.register(
			request.getEmail(),
			encodedPassword,
			request.getName(),
			request.getPhone(),
			RoleName.USER
		);

		log.info("Signup successful for email: {}", request.getEmail());
		return authMapper.toSignupResponse(user, RoleName.USER);
	}
}
