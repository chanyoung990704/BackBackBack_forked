package com.aivle.project.company.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aivle.project.company.dto.CompanyInfoDto;
import com.aivle.project.company.dto.CompanyOverviewResponseDto;
import com.aivle.project.company.dto.CompanySectorDto;
import com.aivle.project.company.service.CompanyOverviewService;
import com.aivle.project.common.security.CurrentUserArgumentResolver;
import com.aivle.project.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;

@WebMvcTest(
	controllers = CompanyOverviewTempController.class,
	properties = {
		"spring.autoconfigure.exclude="
			+ "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
			+ "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
			+ "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration,"
			+ "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"
	}
)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class CompanyOverviewTempControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@org.springframework.boot.test.mock.mockito.MockBean
	private CompanyOverviewService companyOverviewService;

	@org.springframework.boot.test.mock.mockito.MockBean
	private JpaMetamodelMappingContext jpaMetamodelMappingContext;

	@org.springframework.boot.test.mock.mockito.MockBean
	private UserRepository userRepository;

	@org.springframework.boot.test.mock.mockito.MockBean
	private CurrentUserArgumentResolver currentUserArgumentResolver;

	@Test
	@DisplayName("기업 개요 임시 조회 API는 CompanyOverviewResponseDto를 반환한다")
	void getOverview_shouldReturnOverviewResponse() throws Exception {
		// given
		CompanyInfoDto companyInfo = new CompanyInfoDto(
			1L,
			"샘플기업",
			"000001",
			new CompanySectorDto("", "제조업"),
			80.0,
			"SAFE",
			80.0,
			70.0
		);
		CompanyOverviewResponseDto response = new CompanyOverviewResponseDto(
			companyInfo,
			null,
			java.util.List.of(),
			java.util.List.of(),
			"AI 코멘트"
		);

		given(companyOverviewService.getOverview(1L, "202401")).willReturn(response);

		// when & then
		mockMvc.perform(get("/api/companies/1/overview-temp")
				.param("quarterKey", "202401"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.company.name").value("샘플기업"))
			.andExpect(jsonPath("$.data.aiComment").value("AI 코멘트"));
	}
}
