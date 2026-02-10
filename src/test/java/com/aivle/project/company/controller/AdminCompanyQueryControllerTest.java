package com.aivle.project.company.controller;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aivle.project.common.config.TestSecurityConfig;
import com.aivle.project.common.error.CommonErrorCode;
import com.aivle.project.common.error.CommonException;
import com.aivle.project.company.dto.CompanyInfoDto;
import com.aivle.project.company.dto.CompanyOverviewResponseDto;
import com.aivle.project.company.dto.CompanySectorDto;
import com.aivle.project.company.entity.CompaniesEntity;
import com.aivle.project.company.insight.dto.CompanyInsightDto;
import com.aivle.project.company.insight.dto.CompanyInsightType;
import com.aivle.project.company.insight.service.CompanyInsightService;
import com.aivle.project.company.repository.CompaniesRepository;
import com.aivle.project.company.service.CompanyOverviewService;
import com.aivle.project.user.service.AdminUserQueryService;
import com.aivle.project.watchlist.service.CompanyWatchlistService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
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
class AdminCompanyQueryControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private AdminUserQueryService adminUserQueryService;

	@MockBean
	private CompanyWatchlistService companyWatchlistService;

	@MockBean
	private CompanyOverviewService companyOverviewService;

	@MockBean
	private CompanyInsightService companyInsightService;

	@MockBean
	private CompaniesRepository companiesRepository;

	@Test
	@DisplayName("ROLE_ADMIN은 userId 기준 워치리스트 기업 목록을 조회할 수 있다")
	void getCompanies_shouldReturnList() throws Exception {
		// given
		given(companyWatchlistService.getWatchlistCompanies(1L)).willReturn(List.of(
			new CompanyInfoDto(100L, "테스트기업", "000020", new CompanySectorDto("", "제조"), 90.0, "SAFE", 88.0, 77.0)
		));

		// when & then
		mockMvc.perform(get("/api/admin/companies")
				.param("userId", "1")
				.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.length()").value(1))
			.andExpect(jsonPath("$.data[0].id").value(100))
			.andExpect(jsonPath("$.data[0].stockCode").value("000020"));

		then(adminUserQueryService).should().validateActiveUser(eq(1L));
	}

	@Test
	@DisplayName("관리자 기업 개요 조회는 userId 검증 후 기존 응답 DTO를 반환한다")
	void getOverview_shouldReturnOverview() throws Exception {
		// given
		CompaniesEntity company = org.mockito.Mockito.mock(CompaniesEntity.class);
		given(company.getId()).willReturn(100L);
		given(companiesRepository.existsById(20L)).willReturn(false);
		given(companiesRepository.findByStockCode("000020")).willReturn(Optional.of(company));

		CompanyOverviewResponseDto response = new CompanyOverviewResponseDto(
			new CompanyInfoDto(100L, "테스트기업", "000020", new CompanySectorDto("", "제조"), 90.0, "SAFE", 88.0, 77.0),
			null,
			List.of(),
			List.of(),
			"AI 코멘트"
		);
		given(companyOverviewService.getOverview(100L, "202401")).willReturn(response);

		// when & then
		mockMvc.perform(get("/api/admin/companies/000020/overview")
				.param("userId", "1")
				.param("quarterKey", "202401")
				.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.company.id").value(100))
			.andExpect(jsonPath("$.data.aiComment").value("AI 코멘트"));

		then(adminUserQueryService).should().validateActiveUser(eq(1L));
	}

	@Test
	@DisplayName("관리자 기업 인사이트 조회는 기존 응답 구조를 반환한다")
	void getInsights_shouldReturnResponse() throws Exception {
		// given
		List<CompanyInsightDto> items = List.of(
			CompanyInsightDto.builder()
				.id(1L)
				.type(CompanyInsightType.NEWS)
				.title("뉴스")
				.content("본문")
				.source("POS")
				.publishedAt(LocalDateTime.of(2026, 2, 10, 0, 0))
				.url("https://example.com")
				.build()
		);
		given(companyInsightService.getInsights(eq(100L), anyInt(), anyInt(), anyInt(), anyInt(), eq(false)))
			.willReturn(new CompanyInsightService.InsightResult(items, BigDecimal.valueOf(12.34), false));

		// when & then
		mockMvc.perform(get("/api/admin/companies/100/insights")
				.param("userId", "1")
				.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.items.length()").value(1))
			.andExpect(jsonPath("$.data.averageScore").value(12.34));

		then(adminUserQueryService).should().validateActiveUser(eq(1L));
	}

	@Test
	@DisplayName("관리자 기업 인사이트가 처리 중이면 202를 반환한다")
	void getInsights_shouldReturnAcceptedWhenProcessing() throws Exception {
		// given
		given(companyInsightService.getInsights(eq(100L), anyInt(), anyInt(), anyInt(), anyInt(), eq(false)))
			.willReturn(new CompanyInsightService.InsightResult(List.of(), null, true));

		// when & then
		mockMvc.perform(get("/api/admin/companies/100/insights")
				.param("userId", "1")
				.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
			.andExpect(status().isAccepted())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("INSIGHT_PROCESSING"));
	}

	@Test
	@DisplayName("유효하지 않은 userId는 404를 반환한다")
	void getCompanies_shouldReturn404WhenInvalidUser() throws Exception {
		// given
		willThrow(new CommonException(CommonErrorCode.COMMON_404))
			.given(adminUserQueryService)
			.validateActiveUser(99L);

		// when & then
		mockMvc.perform(get("/api/admin/companies")
				.param("userId", "99")
				.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
			.andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("ROLE_USER는 관리자 기업 조회 API에 접근할 수 없다")
	void getCompanies_shouldDenyUserRole() throws Exception {
		// when & then
		mockMvc.perform(get("/api/admin/companies")
				.param("userId", "1")
				.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
			.andExpect(status().isForbidden());
	}
}
