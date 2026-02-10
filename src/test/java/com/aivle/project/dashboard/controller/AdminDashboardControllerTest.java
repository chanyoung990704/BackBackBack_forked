package com.aivle.project.dashboard.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aivle.project.common.config.TestSecurityConfig;
import com.aivle.project.dashboard.dto.DashboardSummaryResponse;
import com.aivle.project.dashboard.dto.RiskStatusDistributionDto;
import com.aivle.project.dashboard.dto.RiskStatusDistributionPercentDto;
import com.aivle.project.dashboard.service.DashboardSummaryService;
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
class AdminDashboardControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private DashboardSummaryService dashboardSummaryService;

	@MockBean
	private AdminUserQueryService adminUserQueryService;

	@Test
	@DisplayName("ROLE_ADMIN은 userId 기준으로 대시보드 요약을 조회할 수 있다")
	void getSummary_shouldReturnDashboardSummary() throws Exception {
		// given
		DashboardSummaryResponse summary = new DashboardSummaryResponse(
			"최근 4분기",
			List.of(),
			"2025Q4",
			"2026Q1",
			List.of("2025Q1", "2025Q2", "2025Q3", "2025Q4", "2026Q1"),
			new RiskStatusDistributionDto(1, 2, 3),
			new RiskStatusDistributionPercentDto(10.0, 20.0, 70.0),
			55.0,
			null,
			List.of()
		);
		given(dashboardSummaryService.getSummary(1L)).willReturn(summary);

		// when & then
		mockMvc.perform(get("/api/admin/dashboard/summary")
				.param("userId", "1")
				.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.range").value("최근 4분기"))
			.andExpect(jsonPath("$.data.latestActualQuarter").value("2025Q4"));

		then(adminUserQueryService).should().validateActiveUser(eq(1L));
		then(dashboardSummaryService).should().getSummary(eq(1L));
	}

	@Test
	@DisplayName("ROLE_USER는 관리자 대시보드 API에 접근할 수 없다")
	void getSummary_shouldDenyUserRole() throws Exception {
		// when & then
		mockMvc.perform(get("/api/admin/dashboard/summary")
				.param("userId", "1")
				.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
			.andExpect(status().isForbidden());
	}
}
