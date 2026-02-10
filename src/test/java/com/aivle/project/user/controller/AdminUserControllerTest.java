package com.aivle.project.user.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aivle.project.common.config.TestSecurityConfig;
import com.aivle.project.user.dto.AdminUserListItemDto;
import com.aivle.project.user.service.AdminUserQueryService;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class AdminUserControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private AdminUserQueryService adminUserQueryService;

	@Test
	@DisplayName("ROLE_ADMIN은 관리자 사용자 목록을 조회할 수 있다")
	void getUsers_shouldReturnUsersForAdmin() throws Exception {
		// given
		given(adminUserQueryService.getActiveUsers()).willReturn(List.of(
			new AdminUserListItemDto(1L, "홍길동", "hong@test.com"),
			new AdminUserListItemDto(2L, "김테스트", "kim@test.com")
		));

		// when & then
		mockMvc.perform(get("/api/admin/users")
				.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.length()").value(2))
			.andExpect(jsonPath("$.data[0].id").value(1))
			.andExpect(jsonPath("$.data[0].name").value("홍길동"))
			.andExpect(jsonPath("$.data[0].email").value("hong@test.com"));
	}

	@Test
	@DisplayName("ROLE_USER는 관리자 사용자 목록에 접근할 수 없다")
	void getUsers_shouldDenyUserRole() throws Exception {
		// when & then
		mockMvc.perform(get("/api/admin/users")
				.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
			.andExpect(status().isForbidden());
	}
}
