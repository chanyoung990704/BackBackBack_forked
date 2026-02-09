package com.aivle.project.company.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.aivle.project.common.config.QuerydslConfig;
import com.aivle.project.company.client.AiServerClient;
import com.aivle.project.company.dto.AiCommentResponse;
import com.aivle.project.company.dto.AiHealthScoreResponse;
import com.aivle.project.company.entity.CompaniesEntity;
import com.aivle.project.company.keymetric.entity.CompanyKeyMetricEntity;
import com.aivle.project.company.keymetric.entity.CompanyKeyMetricRiskLevel;
import com.aivle.project.company.keymetric.repository.CompanyKeyMetricRepository;
import com.aivle.project.company.repository.CompaniesRepository;
import com.aivle.project.industry.entity.IndustryEntity;
import com.aivle.project.industry.entity.IndustryRepository;
import com.aivle.project.metric.entity.MetricValueType;
import com.aivle.project.metric.entity.MetricsEntity;
import com.aivle.project.metric.repository.MetricsRepository;
import com.aivle.project.quarter.entity.QuartersEntity;
import com.aivle.project.quarter.repository.QuartersRepository;
import com.aivle.project.quarter.support.QuarterCalculator;
import com.aivle.project.quarter.support.YearQuarter;
import com.aivle.project.report.entity.CompanyReportMetricValuesEntity;
import com.aivle.project.report.entity.CompanyReportVersionsEntity;
import com.aivle.project.report.entity.CompanyReportsEntity;
import com.aivle.project.report.repository.CompanyReportMetricValuesRepository;
import com.aivle.project.report.repository.CompanyReportVersionsRepository;
import com.aivle.project.report.repository.CompanyReportsRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import({
	QuerydslConfig.class,
	CompanyAiCommentService.class,
	CompanyHealthScoreCacheService.class
})
class CompanyAiCommentServiceTest {

	@Autowired
	private CompanyAiCommentService companyAiCommentService;

	@Autowired
	private CompaniesRepository companiesRepository;

	@Autowired
	private IndustryRepository industryRepository;

	@Autowired
	private QuartersRepository quartersRepository;

	@Autowired
	private MetricsRepository metricsRepository;

	@Autowired
	private CompanyReportsRepository companyReportsRepository;

	@Autowired
	private CompanyReportVersionsRepository companyReportVersionsRepository;

	@Autowired
	private CompanyReportMetricValuesRepository companyReportMetricValuesRepository;

	@Autowired
	private CompanyKeyMetricRepository companyKeyMetricRepository;

	@MockBean
	private AiServerClient aiServerClient;

	@Test
	@DisplayName("동일 기업/동일 분기에 ai_comment가 있으면 DB 값을 반환하고 AI 호출을 생략한다")
	void returnCachedCommentWhenExists() {
		// given
		CompaniesEntity company = createCompany("00000011", "111111");
		QuartersEntity quarter = createQuarter(20251);
		CompanyKeyMetricEntity keyMetric = companyKeyMetricRepository.save(CompanyKeyMetricEntity.create(
			company,
			quarter,
			null,
			BigDecimal.valueOf(70.0),
			null,
			BigDecimal.valueOf(70.0),
			CompanyKeyMetricRiskLevel.WARN,
			1,
			LocalDateTime.now()
		));
		keyMetric.applyAiAnalysis("캐시 코멘트<br>유지", null, null, null, LocalDateTime.now());

		// when
		String result = companyAiCommentService.ensureAiCommentCached(company.getId(), "20251");

		// then
		assertThat(result).isEqualTo("캐시 코멘트<br>유지");
		verifyNoMoreInteractions(aiServerClient);
	}

	@Test
	@DisplayName("period 미지정 시 ACTUAL 최신 분기를 사용해 ai_comment를 저장한다")
	void ensureAiCommentCachedWithLatestActual() {
		// given
		CompaniesEntity company = createCompany("00000012", "222222");
		createActualMetric(company, 20253);

		given(aiServerClient.getHealthScore(company.getStockCode()))
			.willReturn(new AiHealthScoreResponse(
				company.getStockCode(),
				company.getCorpName(),
				List.of(),
				0,
				0
			));
		given(aiServerClient.getAiComment(company.getStockCode(), "20253"))
			.willReturn(new AiCommentResponse(
				company.getStockCode(),
				company.getCorpName(),
				"의약품 제조업",
				"20253",
				"신규 코멘트<br>저장"
			));

		// when
		String result = companyAiCommentService.ensureAiCommentCached(company.getId(), null);

		// then
		assertThat(result).isEqualTo("신규 코멘트<br>저장");
		CompanyKeyMetricEntity saved = companyKeyMetricRepository
			.findByCompanyIdAndQuarter_QuarterKey(company.getId(), 20253)
			.orElseThrow();
		assertThat(saved.getAiComment()).isEqualTo("신규 코멘트<br>저장");
		verify(aiServerClient).getAiComment(company.getStockCode(), "20253");
	}

	private CompaniesEntity createCompany(String corpCode, String stockCode) {
		IndustryEntity industry = industryRepository.save(IndustryEntity.create("A0101", "식품"));
		return companiesRepository.save(CompaniesEntity.create(
			corpCode,
			"테스트기업",
			"TEST_CO",
			stockCode,
			LocalDate.of(2025, 1, 1),
			industry
		));
	}

	private QuartersEntity createQuarter(int quarterKey) {
		YearQuarter yearQuarter = QuarterCalculator.parseQuarterKey(quarterKey);
		return quartersRepository.findByQuarterKey(quarterKey)
			.orElseGet(() -> quartersRepository.save(QuartersEntity.create(
				yearQuarter.year(),
				yearQuarter.quarter(),
				quarterKey,
				QuarterCalculator.startDate(yearQuarter),
				QuarterCalculator.endDate(yearQuarter)
			)));
	}

	private void createActualMetric(CompaniesEntity company, int quarterKey) {
		QuartersEntity quarter = createQuarter(quarterKey);
		CompanyReportsEntity report = companyReportsRepository.save(CompanyReportsEntity.create(company, quarter, null));
		CompanyReportVersionsEntity version = companyReportVersionsRepository.save(CompanyReportVersionsEntity.create(
			report,
			1,
			LocalDateTime.now(),
			true,
			null
		));
		MetricsEntity metric = metricsRepository.findByMetricCode("ROA")
			.orElseGet(() -> metricsRepository.save(MetricsEntity.create("ROA", "ROA", "ROA", true)));

		companyReportMetricValuesRepository.save(CompanyReportMetricValuesEntity.create(
			version,
			metric,
			quarter,
			BigDecimal.ONE,
			MetricValueType.ACTUAL
		));
	}
}
