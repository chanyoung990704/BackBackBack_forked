package com.aivle.project.company.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import com.aivle.project.company.dto.CompanyInfoDto;
import com.aivle.project.company.dto.CompanySectorDto;
import com.aivle.project.company.entity.CompaniesEntity;
import com.aivle.project.company.repository.CompaniesRepository;
import com.aivle.project.common.config.TestSecurityConfig;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
@Transactional
class CompanySearchControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private CompaniesRepository companiesRepository;

	@MockBean
	private com.aivle.project.watchlist.service.CompanyWatchlistService companyWatchlistService;

	@Test
	@DisplayName("ROLE_USER로 워치리스트 기업 목록을 조회한다")
	void getMyCompanies() throws Exception {
		// given
		given(companyWatchlistService.getWatchlistCompanies(any()))
			.willReturn(List.of(
				new CompanyInfoDto(
					10L,
					"테스트기업",
					"000020",
					new CompanySectorDto("", "테스트업종"),
					90.0,
					"SAFE",
					88.0,
					77.0
				)
			));

		// when & then
		mockMvc.perform(get("/api/companies")
				.with(jwt().jwt(builder -> builder.claim("userId", 1L)).authorities(new SimpleGrantedAuthority("ROLE_USER"))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.length()").value(1))
			.andExpect(jsonPath("$.data[0].stockCode").value("000020"));
	}

	@Test
	@DisplayName("ROLE_USER로 기업명을 검색한다")
	void searchCompanies() throws Exception {
		// given
		companiesRepository.save(CompaniesEntity.create(
			"00000001",
			"테스트기업",
			"TEST_CO",
			"000020",
			LocalDate.of(2025, 1, 1)
		));

		// when & then
		mockMvc.perform(get("/api/companies/search")
					.param("keyword", "테스트")
					.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.length()").value(1))
				.andExpect(jsonPath("$.data[0].stockCode").value("000020"));
	}

	@Test
	@DisplayName("ROLE_USER로 기업명을 query 파라미터로 검색한다")
	void searchCompanies_withQueryParam() throws Exception {
		// given
		companiesRepository.save(CompaniesEntity.create(
			"00000002",
			"테스트기업2",
			"TEST_CO2",
			"000021",
			LocalDate.of(2025, 1, 1)
		));

		// when & then
		mockMvc.perform(get("/api/companies/search")
				.param("query", "테스트")
				.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.length()").value(1))
			.andExpect(jsonPath("$.data[0].stockCode").value("000021"));
	}

	@Test
	@DisplayName("ROLE_USER로 신규 내 기업 경로(/api/companies/me)를 조회한다")
	void getMyCompanies_withMePath() throws Exception {
		// given
		given(companyWatchlistService.getWatchlistCompanies(any()))
			.willReturn(List.of(
				new CompanyInfoDto(
					10L,
					"테스트기업",
					"000020",
					new CompanySectorDto("", "테스트업종"),
					90.0,
					"SAFE",
					88.0,
					77.0
				)
			));

		// when & then
		mockMvc.perform(get("/api/companies/me")
				.with(jwt().jwt(builder -> builder.claim("userId", 1L)).authorities(new SimpleGrantedAuthority("ROLE_USER"))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.length()").value(1))
			.andExpect(jsonPath("$.data[0].stockCode").value("000020"));
	}

	@Test
	@DisplayName("기업명 검색은 인증 없이도 가능하다 (PermitAll)")
	void searchCompanies_public() throws Exception {
		// when & then
		mockMvc.perform(get("/api/companies/search")
				.param("keyword", "테스트"))
			.andExpect(status().isOk());
	}

	@Test
	@DisplayName("기업명 검색은 ROLE_ADMIN도 접근 가능하다 (권한 계층)")
	void searchCompanies_allowedForAdmin() throws Exception {
		// when & then
		mockMvc.perform(get("/api/companies/search")
				.param("keyword", "테스트")
				.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
			.andExpect(status().isOk());
	}
}
