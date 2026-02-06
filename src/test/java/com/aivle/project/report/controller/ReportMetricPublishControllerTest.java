package com.aivle.project.report.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aivle.project.company.entity.CompaniesEntity;
import com.aivle.project.company.repository.CompaniesRepository;
import com.aivle.project.common.config.TestSecurityConfig;
import com.aivle.project.metric.entity.MetricValueType;
import com.aivle.project.report.entity.CompanyReportMetricValuesEntity;
import com.aivle.project.report.entity.CompanyReportVersionsEntity;
import com.aivle.project.report.repository.CompanyReportMetricValuesRepository;
import com.aivle.project.report.repository.CompanyReportVersionsRepository;
import com.aivle.project.report.repository.CompanyReportsRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
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
class ReportMetricPublishControllerTest {

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
	@DisplayName("관리자 API로 지표를 수동 적재한다")
	void publishMetrics_shouldPersistValues() throws Exception {
		// given
		companiesRepository.save(CompaniesEntity.create(
			"00000001",
			"테스트기업",
			"TEST_CO",
			"000020",
			LocalDate.of(2025, 1, 1)
		));
		Map<String, BigDecimal> metrics = new LinkedHashMap<>();
		metrics.put("ROA", new BigDecimal("1.23"));
		metrics.put("OperatingProfitMargin", new BigDecimal("2.34"));
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("stockCode", "000020");
		payload.put("quarterKey", 20253);
		payload.put("valueType", MetricValueType.ACTUAL.name());
		payload.put("metrics", metrics);

		// when
		mockMvc.perform(post("/api/admin/reports/metrics")
				.contentType(MediaType.APPLICATION_JSON)
				.content(new ObjectMapper().writeValueAsBytes(payload))
				.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
			.andExpect(status().isOk());

		// then
		assertThat(companyReportsRepository.count()).isEqualTo(1);
		assertThat(companyReportVersionsRepository.count()).isEqualTo(1);
		assertThat(companyReportMetricValuesRepository.count()).isEqualTo(2);
		CompanyReportVersionsEntity version = companyReportVersionsRepository.findAll().get(0);
		assertThat(version.isPublished()).isFalse();
		assertThat(version.getPdfFile()).isNull();
		for (CompanyReportMetricValuesEntity value : companyReportMetricValuesRepository.findAll()) {
			assertThat(value.getValueType()).isEqualTo(MetricValueType.ACTUAL);
		}
	}
}
