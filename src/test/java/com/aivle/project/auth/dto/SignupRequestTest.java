package com.aivle.project.auth.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SignupRequestTest {

	private Validator validator;

	@BeforeEach
	void setUp() {
		ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
		validator = factory.getValidator();
	}

	@Test
	@DisplayName("비밀번호 3종 조합(영문, 숫자, 특수문자) - 8자 이상 성공")
	void password_threeTypes_min8_success() {
		// given
		SignupRequest request = createValidRequest();
		request.setPassword("Ab1!asdf"); // 대문자, 소문자, 숫자, 특수문자 포함 (8자)

		// when
		Set<ConstraintViolation<SignupRequest>> violations = validator.validate(request);

		// then
		assertThat(violations).isEmpty();
	}

	@Test
	@DisplayName("비밀번호 2종 조합 - 10자 이상 성공")
	void password_twoTypes_min10_success() {
		// given
		SignupRequest request = createValidRequest();
		request.setPassword("Ab1asdfghj"); // 영문, 숫자 포함 (10자)

		// when
		Set<ConstraintViolation<SignupRequest>> violations = validator.validate(request);

		// then
		assertThat(violations).isEmpty();
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"Ab1!asdf",    // 3종 조합, 8자 -> 성공 케이스 (중복 확인용으로 남김)
		"Ab1asdfghj",  // 2종 조합, 10자 -> 성공 케이스
	})
	@DisplayName("비밀번호 복잡도 충족 시 성공")
	void password_validation_success(String password) {
		// given
		SignupRequest request = createValidRequest();
		request.setPassword(password);

		// when
		Set<ConstraintViolation<SignupRequest>> violations = validator.validate(request);

		// then
		assertThat(violations).isEmpty();
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"Ab1!",        // 3종 조합이나 8자 미만
		"Ab1asdfgh",   // 2종 조합이나 10자 미만
		"aaa111!!!a",  // 3회 연속 문자 포함 (aaa)
		"Abasdfghjk",  // 1종 조합(영문) 10자 -> 실패
	})
	@DisplayName("비밀번호 복잡도 미달 또는 연속 문자 포함 시 실패")
	void password_validation_fail(String password) {
		// given
		SignupRequest request = createValidRequest();
		request.setPassword(password);

		// when
		Set<ConstraintViolation<SignupRequest>> violations = validator.validate(request);

		// then
		assertThat(violations).isNotEmpty();
	}

	@Test
	@DisplayName("비밀번호에 전화번호 포함 시 실패 (@AssertTrue)")
	void password_contains_phone_fail() {
		// given
		SignupRequest request = createValidRequest();
		request.setPhone("01012345678");
		request.setPassword("pass01012345678!"); // 전화번호 포함

		// when
		Set<ConstraintViolation<SignupRequest>> violations = validator.validate(request);

		// then
		assertThat(violations).anyMatch(v -> v.getMessage().contains("전화번호"));
	}

	private SignupRequest createValidRequest() {
		SignupRequest request = new SignupRequest();
		request.setEmail("test@example.com");
		request.setName("테스터");
		request.setPhone("01011112222");
		return request;
	}
}
