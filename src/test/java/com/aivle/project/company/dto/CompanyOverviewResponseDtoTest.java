package com.aivle.project.company.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CompanyOverviewResponseDtoTest {

	@Test
	@DisplayName("CompanyOverviewResponseDto는 전달된 데이터를 보관한다")
	void shouldHoldCompanyOverviewResponseData() {
		// given
		CompanyOverviewTooltipDto tooltip = new CompanyOverviewTooltipDto(
			"설명",
			"해석",
			"힌트"
		);
		CompanyOverviewKeyMetricDto keyMetric = new CompanyOverviewKeyMetricDto(
			"NETWORK_HEALTH",
			"내부 건강도",
			75.0,
			"%",
			tooltip
		);
		CompanyOverviewMetricDto metric = new CompanyOverviewMetricDto(
			"DEBT_RATIO",
			"부채비율",
			CompanyOverviewSignalLevel.GREEN,
			22.5,
			"%",
			tooltip
		);
		CompanyOverviewDataPointDto dataPoint = new CompanyOverviewDataPointDto(
			"2024Q4",
			10.0,
			CompanyOverviewDataType.ACTUAL
		);
		CompanyOverviewMetricSeriesDto metricSeries = new CompanyOverviewMetricSeriesDto(
			"REVENUE",
			"매출",
			"억원",
			List.of(dataPoint)
		);
		CompanyOverviewForecastDto forecast = new CompanyOverviewForecastDto(
			"2024Q4",
			"2025Q1",
			List.of(metricSeries)
		);
		CompanyInfoDto company = new CompanyInfoDto(
			1L,
			"테스트기업",
			"000000",
			new CompanySectorDto("", "제조"),
			75.0,
			"SAFE",
			75.0,
			60.0
		);

		// when
		CompanyOverviewResponseDto response = new CompanyOverviewResponseDto(
			company,
			forecast,
			List.of(keyMetric),
			List.of(metric),
			"AI 코멘트"
		);

		// then
		assertThat(response.getCompany()).isEqualTo(company);
		assertThat(response.getForecast()).isEqualTo(forecast);
		assertThat(response.getKeyMetrics()).containsExactly(keyMetric);
		assertThat(response.getMetrics()).containsExactly(metric);
		assertThat(response.getAiComment()).isEqualTo("AI 코멘트");
	}
}
