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
import org.springframework.security.crypto.password.PasswordEncoder;

import com.aivle.project.auth.mapper.AuthMapper;
import com.aivle.project.user.service.UserDomainService;

@ExtendWith(MockitoExtension.class)
class SignUpServiceTest {

	@Mock
	private UserDomainService userDomainService;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private AuthMapper authMapper;

	@Test
	@DisplayName("회원가입 시 사용자와 USER 역할을 저장한다")
	void signup_shouldPersistUserAndRole() {
		// given: 회원가입 요청과 역할 정보를 준비
		SignUpService signUpService = new SignUpService(userDomainService, passwordEncoder, authMapper);
		SignupRequest request = new SignupRequest();
		request.setEmail("new@test.com");
		request.setPassword("password123");
		request.setName("tester");
		request.setPhone("01012345678");

		UserEntity user = UserEntity.create("new@test.com", "encoded", "tester", "01012345678", UserStatus.ACTIVE);
		SignupResponse signupResponse = new SignupResponse(1L, java.util.UUID.randomUUID(), "new@test.com", UserStatus.ACTIVE, RoleName.USER);

		when(userDomainService.existsByEmail("new@test.com")).thenReturn(false);
		when(passwordEncoder.encode("password123")).thenReturn("encoded");
		when(userDomainService.register("new@test.com", "encoded", "tester", "01012345678", RoleName.USER))
			.thenReturn(user);
		when(authMapper.toSignupResponse(user, RoleName.USER)).thenReturn(signupResponse);

		// when: 회원가입을 수행
		SignupResponse response = signUpService.signup(request);

		// then: 사용자 저장과 매핑이 수행된다
		assertThat(response.email()).isEqualTo("new@test.com");
		verify(userDomainService).register("new@test.com", "encoded", "tester", "01012345678", RoleName.USER);
	}

	@Test
	@DisplayName("이미 존재하는 이메일이면 회원가입이 실패한다")
	void signup_shouldFailWhenEmailExists() {
		// given: 기존 사용자 이메일을 준비
		SignUpService signUpService = new SignUpService(userDomainService, passwordEncoder, authMapper);
		SignupRequest request = new SignupRequest();
		request.setEmail("dup@test.com");
		request.setPassword("password123");
		request.setName("tester");

		when(userDomainService.existsByEmail("dup@test.com")).thenReturn(true);

		// when & then: 예외가 발생한다
		assertThatThrownBy(() -> signUpService.signup(request))
			.isInstanceOf(AuthException.class)
			.hasMessage(AuthErrorCode.EMAIL_ALREADY_EXISTS.getMessage());
	}
}
