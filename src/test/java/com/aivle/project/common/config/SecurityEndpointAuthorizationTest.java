package com.aivle.project.common.config;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aivle.project.company.service.CompanySearchService;
import com.aivle.project.dashboard.service.DashboardSummaryService;
import com.aivle.project.metricaverage.service.MetricAverageBatchService;
import com.aivle.project.user.dto.AdminUserListItemDto;
import com.aivle.project.user.service.AdminUserQueryService;
import com.aivle.project.watchlist.service.CompanyWatchlistService;
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
class SecurityEndpointAuthorizationTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private CompanySearchService companySearchService;

	@MockBean
	private CompanyWatchlistService companyWatchlistService;

	@MockBean
	private DashboardSummaryService dashboardSummaryService;

	@MockBean
	private MetricAverageBatchService metricAverageBatchService;

	@MockBean
	private AdminUserQueryService adminUserQueryService;

	@Test
	@DisplayName("공개 검색 API는 인증 없이 접근 가능하다")
	void companySearchEndpoint_shouldPermitAll() throws Exception {
		// given
		given(companySearchService.search("삼성")).willReturn(List.of());

		// when & then
		mockMvc.perform(get("/api/companies/search").param("keyword", "삼성"))
			.andExpect(status().isOk());
	}

	@Test
	@DisplayName("대시보드 리스크 레코드 API는 인증이 없으면 401을 반환한다")
	void dashboardRiskRecordsEndpoint_shouldRequireAuthentication() throws Exception {
		// when & then
		mockMvc.perform(get("/api/dashboard/risk-records"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("대시보드 리스크 레코드 API는 ROLE_USER 토큰으로 접근 가능하다")
	void dashboardRiskRecordsEndpoint_shouldAllowUserRole() throws Exception {
		// given
		given(dashboardSummaryService.getRiskRecords(1L, 200)).willReturn(List.of());

		// when & then
		mockMvc.perform(get("/api/dashboard/risk-records")
				.with(jwt().jwt(jwt -> jwt.claim("userId", 1L))
					.authorities(new SimpleGrantedAuthority("ROLE_USER"))))
			.andExpect(status().isOk());
	}

	@Test
	@DisplayName("관리자 API는 ROLE_USER 토큰으로 접근하면 403을 반환한다")
	void adminEndpoint_shouldDenyUserRole() throws Exception {
		// when & then
		mockMvc.perform(post("/api/admin/metric-averages/initialize")
				.with(jwt().jwt(jwt -> jwt.claim("userId", 1L))
					.authorities(new SimpleGrantedAuthority("ROLE_USER"))))
			.andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("관리자 사용자 목록 API는 ROLE_ADMIN 토큰으로 접근 가능하다")
	void adminUsersEndpoint_shouldAllowAdminRole() throws Exception {
		// given
		given(adminUserQueryService.getActiveUsers())
			.willReturn(List.of(new AdminUserListItemDto(1L, "관리자", "admin@test.com")));

		// when & then
		mockMvc.perform(get("/api/admin/users")
				.with(jwt().jwt(jwt -> jwt.claim("userId", 1L))
					.authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
			.andExpect(status().isOk());
	}

	@Test
	@DisplayName("관리자 사용자 목록 API는 ROLE_USER 토큰으로 접근하면 403을 반환한다")
	void adminUsersEndpoint_shouldDenyUserRole() throws Exception {
		// when & then
		mockMvc.perform(get("/api/admin/users")
				.with(jwt().jwt(jwt -> jwt.claim("userId", 1L))
					.authorities(new SimpleGrantedAuthority("ROLE_USER"))))
			.andExpect(status().isForbidden());
	}
}
