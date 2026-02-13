package com.aivle.project.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aivle.project.auth.dto.SignupRequest;
import com.aivle.project.auth.dto.SignupResponse;
import com.aivle.project.auth.exception.AuthErrorCode;
import com.aivle.project.auth.exception.AuthException;
import com.aivle.project.user.entity.RoleEntity;
import com.aivle.project.user.entity.RoleName;
import com.aivle.project.user.entity.UserEntity;
import com.aivle.project.user.entity.UserRoleEntity;
import com.aivle.project.user.entity.UserStatus;
import com.aivle.project.user.repository.RoleRepository;
import com.aivle.project.user.repository.UserRepository;
import com.aivle.project.user.repository.UserRoleRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.aivle.project.auth.mapper.AuthMapper;
import com.aivle.project.user.service.EmailVerificationService;
import com.aivle.project.user.service.UserDomainService;

@ExtendWith(MockitoExtension.class)
class SignUpServiceTest {

	@Mock
	private UserDomainService userDomainService;

	@Mock
	private EmailVerificationService emailVerificationService;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private AuthMapper authMapper;

	@Mock
	private TurnstileService turnstileService;

	@Test
	@DisplayName("회원가입 시 사용자와 USER 역할을 저장한다")
	void signup_shouldPersistUserAndRole() {
		// given: 회원가입 요청과 역할 정보를 준비
		SignUpService signUpService = new SignUpService(userDomainService, emailVerificationService, turnstileService, passwordEncoder, authMapper);
		SignupRequest request = new SignupRequest();
		request.setEmail("new@test.com");
		request.setPassword("password123");
		request.setName("tester");
		request.setTurnstileToken("valid-token");

		UserEntity user = UserEntity.create("new@test.com", "encoded", "tester", null, UserStatus.ACTIVE);
		SignupResponse signupResponse = new SignupResponse(1L, java.util.UUID.randomUUID(), "new@test.com", UserStatus.ACTIVE, RoleName.ROLE_USER);

		when(turnstileService.verifyTokenSync("valid-token", "127.0.0.1")).thenReturn(true);
		when(userDomainService.existsByEmail("new@test.com")).thenReturn(false);
		when(passwordEncoder.encode("password123")).thenReturn("encoded");
		when(userDomainService.register("new@test.com", "encoded", request.getName(), null, RoleName.ROLE_USER))
			.thenReturn(user);
		when(authMapper.toSignupResponse(user, RoleName.ROLE_USER)).thenReturn(signupResponse);

		// when: 회원가입을 수행
		SignupResponse response = signUpService.signup(request, "127.0.0.1");

		// then: 사용자 저장과 매핑이 수행된다
		assertThat(response.email()).isEqualTo("new@test.com");
		verify(userDomainService).register("new@test.com", "encoded", "tester", null, RoleName.ROLE_USER);
	}

	@Test
	@DisplayName("회원가입 시 인증 이메일이 발송된다")
	void signup_shouldSendVerificationEmail() {
		// given: 회원가입 요청과 사용자 상태를 준비
		SignUpService signUpService = new SignUpService(userDomainService, emailVerificationService, turnstileService, passwordEncoder, authMapper);
		SignupRequest request = new SignupRequest();
		request.setEmail("verify@test.com");
		request.setPassword("password123");
		request.setName("tester");
		request.setTurnstileToken("valid-token");

		UserEntity user = UserEntity.create("verify@test.com", "encoded", "tester", null, UserStatus.PENDING);
		SignupResponse signupResponse = new SignupResponse(1L, java.util.UUID.randomUUID(), "verify@test.com", UserStatus.PENDING, RoleName.ROLE_USER);

		when(turnstileService.verifyTokenSync("valid-token", "127.0.0.1")).thenReturn(true);
		when(userDomainService.existsByEmail("verify@test.com")).thenReturn(false);
		when(passwordEncoder.encode("password123")).thenReturn("encoded");
		when(userDomainService.register("verify@test.com", "encoded", request.getName(), null, RoleName.ROLE_USER))
			.thenReturn(user);
		when(authMapper.toSignupResponse(user, RoleName.ROLE_USER)).thenReturn(signupResponse);

		// when: 회원가입을 수행
		SignupResponse response = signUpService.signup(request, "127.0.0.1");

		// then: 인증 이메일 발송이 수행된다
		assertThat(response.email()).isEqualTo("verify@test.com");
		verify(emailVerificationService).sendVerificationEmail(user, "verify@test.com");
	}

	@Test
	@DisplayName("dev 환경에서는 이메일 인증을 생략하고 사용자를 활성화한다")
	void signup_shouldSkipEmailVerificationWhenConfigured() {
		// given: 이메일 인증 스킵 설정과 회원가입 요청을 준비
		SignUpService signUpService = new SignUpService(userDomainService, emailVerificationService, turnstileService, passwordEncoder, authMapper);
		ReflectionTestUtils.setField(signUpService, "skipEmailVerification", true);

		SignupRequest request = new SignupRequest();
		request.setEmail("dev@test.com");
		request.setPassword("password123");
		request.setName("tester");
		request.setTurnstileToken("valid-token");

		UserEntity user = UserEntity.create("dev@test.com", "encoded", "tester", null, UserStatus.PENDING);
		ReflectionTestUtils.setField(user, "id", 1L);
		SignupResponse signupResponse = new SignupResponse(1L, java.util.UUID.randomUUID(), "dev@test.com", UserStatus.ACTIVE, RoleName.ROLE_USER);

		when(turnstileService.verifyTokenSync("valid-token", "127.0.0.1")).thenReturn(true);
		when(userDomainService.existsByEmail("dev@test.com")).thenReturn(false);
		when(passwordEncoder.encode("password123")).thenReturn("encoded");
		when(userDomainService.register("dev@test.com", "encoded", request.getName(), null, RoleName.ROLE_USER))
			.thenReturn(user);
		when(authMapper.toSignupResponse(user, RoleName.ROLE_USER)).thenReturn(signupResponse);

		// when: 회원가입을 수행
		SignupResponse response = signUpService.signup(request, "127.0.0.1");

		// then: 이메일 인증 전송 없이 사용자 활성화가 호출된다
		assertThat(response.email()).isEqualTo("dev@test.com");
		verify(userDomainService).activateUser(1L);
		verify(emailVerificationService, org.mockito.Mockito.never()).sendVerificationEmail(user, "dev@test.com");
	}

	@Test
	@DisplayName("이미 존재하는 이메일이면 회원가입이 실패한다")
	void signup_shouldFailWhenEmailExists() {
		// given: 기존 사용자 이메일을 준비
		SignUpService signUpService = new SignUpService(userDomainService, emailVerificationService, turnstileService, passwordEncoder, authMapper);
		SignupRequest request = new SignupRequest();
		request.setEmail("dup@test.com");
		request.setPassword("password123");
		request.setName("tester");
		request.setTurnstileToken("valid-token");

		when(turnstileService.verifyTokenSync("valid-token", "127.0.0.1")).thenReturn(true);
		when(userDomainService.existsByEmail("dup@test.com")).thenReturn(true);

		// when & then: 예외가 발생한다
		assertThatThrownBy(() -> signUpService.signup(request, "127.0.0.1"))
			.isInstanceOf(AuthException.class)
			.hasMessage(AuthErrorCode.EMAIL_ALREADY_EXISTS.getMessage());
	}

	@Test
	@DisplayName("회원가입 저장 중 이메일 유니크 충돌이 발생하면 도메인 예외로 변환한다")
	void signup_shouldMapDuplicateEmailConflictToDomainError() {
		SignUpService signUpService = new SignUpService(userDomainService, emailVerificationService, turnstileService, passwordEncoder, authMapper);
		SignupRequest request = new SignupRequest();
		request.setEmail("race@test.com");
		request.setPassword("password123");
		request.setName("tester");
		request.setTurnstileToken("valid-token");

		when(turnstileService.verifyTokenSync("valid-token", "127.0.0.1")).thenReturn(true);
		when(userDomainService.existsByEmail("race@test.com")).thenReturn(false);
		when(passwordEncoder.encode("password123")).thenReturn("encoded");
		when(userDomainService.register("race@test.com", "encoded", request.getName(), null, RoleName.ROLE_USER))
			.thenThrow(new DataIntegrityViolationException("uk_users_email"));

		assertThatThrownBy(() -> signUpService.signup(request, "127.0.0.1"))
			.isInstanceOf(AuthException.class)
			.hasMessage(AuthErrorCode.EMAIL_ALREADY_EXISTS.getMessage());
	}
}
