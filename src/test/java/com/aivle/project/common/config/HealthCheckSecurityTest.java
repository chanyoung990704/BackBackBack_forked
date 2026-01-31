package com.aivle.project.common.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import com.aivle.project.auth.service.AccessTokenBlacklistService;
import com.aivle.project.common.security.RestAccessDeniedHandler;
import com.aivle.project.common.security.RestAuthenticationEntryPoint;
import com.aivle.project.user.repository.UserRepository;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.security.core.userdetails.UserDetailsService;

@WebMvcTest(controllers = TestHealthController.class)
@AutoConfigureMockMvc
@Import({SecurityConfig.class, TestSecurityConfig.class})
@org.springframework.test.context.TestPropertySource(properties = {
	"jwt.issuer=test-issuer",
	"jwt.access-token.expiration=1800",
	"jwt.refresh-token.expiration=604800",
	"jwt.keys.private-key-path=/tmp/private.pem",
	"jwt.keys.public-key-path=/tmp/public.pem",
	"jwt.keys.current-kid=test-key"
})
@ActiveProfiles("test")
class HealthCheckSecurityTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private UserRepository userRepository;

	@MockBean
	private JpaMetamodelMappingContext jpaMetamodelMappingContext;

	@MockBean
	private AccessTokenBlacklistService accessTokenBlacklistService;

	@MockBean
	private RestAuthenticationEntryPoint restAuthenticationEntryPoint;

	@MockBean
	private RestAccessDeniedHandler restAccessDeniedHandler;

	@MockBean
	private UserDetailsService userDetailsService;

	@Test
	@DisplayName("헬스체크 경로는 인증 없이 접근 가능하다")
	void healthEndpoint_shouldPermitAll() throws Exception {
		mockMvc.perform(get("/actuator/health"))
			.andExpect(status().isOk());
	}

}
