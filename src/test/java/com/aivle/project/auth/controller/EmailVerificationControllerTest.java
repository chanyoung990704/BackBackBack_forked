package com.aivle.project.auth.controller;

import static org.mockito.BDDMockito.doThrow;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
	@DisplayName("redirect=true면 실패 사유를 포함해 프론트로 리다이렉트한다")
	void verifyEmail_redirectsOnFailure() throws Exception {
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
				"https://front.example.com/auth/verify-email?status=fail&reason=expired"));
	}
}
