package com.aivle.project.auth.controller;

import static org.mockito.BDDMockito.doThrow;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import com.aivle.project.user.repository.UserRepository;
import com.aivle.project.user.service.EmailVerificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
	controllers = EmailVerificationController.class,
	properties = "app.email.verification.redirect-base-url=https://front.example.com"
)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class EmailVerificationControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private EmailVerificationService emailVerificationService;

	@MockBean
	private UserRepository userRepository;

	@MockBean
	private JpaMetamodelMappingContext jpaMetamodelMappingContext;

	@Test
	@DisplayName("redirect=true면 성공 시 프론트로 리다이렉트한다")
	void verifyEmail_redirectsOnSuccess() throws Exception {
		// given
		willDoNothing().given(emailVerificationService).verifyEmail("token");

		// when & then
		mockMvc.perform(get("/api/auth/verify-email")
				.param("token", "token")
				.param("redirect", "true"))
			.andExpect(status().isFound())
			.andExpect(header().string("Location",
				"https://front.example.com/auth/verify-email?status=success"));
	}

	@Test
	@DisplayName("redirect=true면 만료 토큰 실패 시 status=expired로 리다이렉트한다")
	void verifyEmail_redirectsOnExpiredFailure() throws Exception {
		// given
		doThrow(new IllegalArgumentException("인증 토큰이 만료되었습니다."))
			.when(emailVerificationService)
			.verifyEmail("token");

		// when & then
		mockMvc.perform(get("/api/auth/verify-email")
				.param("token", "token")
				.param("redirect", "true"))
			.andExpect(status().isFound())
			.andExpect(header().string("Location",
				"https://front.example.com/auth/verify-email?status=expired"));
	}

	@Test
	@DisplayName("redirect=true면 이미 인증된 이메일은 status=already_verified로 리다이렉트한다")
	void verifyEmail_redirectsOnAlreadyVerified() throws Exception {
		// given
		doThrow(new IllegalArgumentException("이미 인증된 이메일입니다."))
			.when(emailVerificationService)
			.verifyEmail("token");

		// when & then
		mockMvc.perform(get("/api/auth/verify-email")
				.param("token", "token")
				.param("redirect", "true"))
			.andExpect(status().isFound())
			.andExpect(header().string("Location",
				"https://front.example.com/auth/verify-email?status=already_verified"));
	}

	@Test
	@DisplayName("redirect=true + returnUrl이 허용 도메인이면 해당 URL로 상태를 전달한다")
	void verifyEmail_redirectsToAllowedReturnUrl() throws Exception {
		// given
		willDoNothing().given(emailVerificationService).verifyEmail("token");

		// when & then
		mockMvc.perform(get("/api/auth/verify-email")
				.param("token", "token")
				.param("redirect", "true")
				.param("returnUrl", "https://front.example.com/auth/signup"))
			.andExpect(status().isFound())
			.andExpect(header().string("Location",
				"https://front.example.com/auth/signup?status=success"));
	}

	@Test
	@DisplayName("redirect=true + returnUrl이 외부 도메인이면 기본 경로로 강제한다")
	void verifyEmail_fallbacksWhenReturnUrlIsNotWhitelisted() throws Exception {
		// given
		willDoNothing().given(emailVerificationService).verifyEmail("token");

		// when & then
		mockMvc.perform(get("/api/auth/verify-email")
				.param("token", "token")
				.param("redirect", "true")
				.param("returnUrl", "https://evil.example.com/steal"))
			.andExpect(status().isFound())
			.andExpect(header().string("Location",
				"https://front.example.com/auth/verify-email?status=success"));
	}

	@Test
	@DisplayName("redirect=false면 JSON 성공 응답을 반환한다")
	void verifyEmail_returnsPlainMessageWhenRedirectFalse() throws Exception {
		// given
		willDoNothing().given(emailVerificationService).verifyEmail("token");

		// when & then
		mockMvc.perform(get("/api/auth/verify-email")
				.param("token", "token"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.status").value("success"))
			.andExpect(jsonPath("$.data.message").value("이메일 인증이 완료되었습니다."));
	}

	@Test
	@DisplayName("redirect=false면 JSON 실패 응답을 반환한다")
	void verifyEmail_returnsJsonFailureWhenRedirectFalse() throws Exception {
		// given
		doThrow(new IllegalArgumentException("유효하지 않은 인증 토큰입니다."))
			.when(emailVerificationService)
			.verifyEmail("token");

		// when & then
		mockMvc.perform(get("/api/auth/verify-email")
				.param("token", "token"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("EMAIL_VERIFICATION_INVALID"))
			.andExpect(jsonPath("$.error.message").value("유효하지 않은 인증 토큰입니다."));
	}

	@Test
	@DisplayName("재전송 성공 시 공통 응답 포맷으로 반환한다")
	void resendVerification_returnsApiResponseOnSuccess() throws Exception {
		// given
		willDoNothing().given(emailVerificationService).resendVerificationEmail(1L);

		// when & then
		mockMvc.perform(get("/api/auth/resend-verification")
				.param("userId", "1"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.status").value("success"))
			.andExpect(jsonPath("$.data.message").value("인증 이메일이 재전송되었습니다."));
	}

	@Test
	@DisplayName("재전송 실패 시 공통 에러 응답을 반환한다")
	void resendVerification_returnsApiErrorOnFailure() throws Exception {
		// given
		doThrow(new IllegalArgumentException("존재하지 않는 사용자입니다."))
			.when(emailVerificationService)
			.resendVerificationEmail(1L);

		// when & then
		mockMvc.perform(get("/api/auth/resend-verification")
				.param("userId", "1"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("EMAIL_VERIFICATION_RESEND_FAILED"))
			.andExpect(jsonPath("$.error.message").value("존재하지 않는 사용자입니다."));
	}
}
