package com.aivle.project.company.report.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aivle.project.company.entity.CompaniesEntity;
import com.aivle.project.company.repository.CompaniesRepository;
import com.aivle.project.common.config.TestSecurityConfig;
import com.aivle.project.company.metric.entity.MetricValueType;
import com.aivle.project.company.report.entity.CompanyReportMetricValuesEntity;
import com.aivle.project.company.report.repository.CompanyReportMetricValuesRepository;
import com.aivle.project.company.report.repository.CompanyReportVersionsRepository;
import com.aivle.project.company.report.repository.CompanyReportsRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
@Transactional
class ReportMetricPredictControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private CompaniesRepository companiesRepository;

	@Autowired
	private CompanyReportsRepository companyReportsRepository;

	@Autowired
	private CompanyReportVersionsRepository companyReportVersionsRepository;

	@Autowired
	private CompanyReportMetricValuesRepository companyReportMetricValuesRepository;

	@Test
	@DisplayName("관리자 API로 예측 지표를 적재한다")
	void importPredictedMetrics_shouldPersistValues() throws Exception {
		// given
		companiesRepository.save(CompaniesEntity.create(
			"00000001",
			"테스트기업",
			"TEST_CO",
			"000020",
			LocalDate.of(2025, 1, 1)
		));
		Map<String, Object> metrics = new LinkedHashMap<>();
		metrics.put("ROA", 1.23);
		metrics.put("OperatingProfitMargin", 2.34);
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("stockCode", "000020");
		payload.put("quarterKey", 20253);
		payload.put("metrics", metrics);

		// when
		mockMvc.perform(post("/api/admin/reports/metrics/predict")
				.contentType(MediaType.APPLICATION_JSON)
				.content(new ObjectMapper().writeValueAsBytes(payload))
				.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
			.andExpect(status().isOk());

		// then
		assertThat(companyReportsRepository.count()).isEqualTo(1);
		assertThat(companyReportVersionsRepository.count()).isEqualTo(1);
		assertThat(companyReportMetricValuesRepository.count()).isEqualTo(2);
		for (CompanyReportMetricValuesEntity value : companyReportMetricValuesRepository.findAll()) {
			assertThat(value.getValueType()).isEqualTo(MetricValueType.PREDICTED);
		}
	}
}
