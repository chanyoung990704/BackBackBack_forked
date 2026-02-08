package com.aivle.project.metric.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.aivle.project.common.config.QuerydslConfig;
import com.aivle.project.metric.entity.MetricDescriptionEntity;
import com.aivle.project.metric.entity.MetricsEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import(QuerydslConfig.class)
class MetricDescriptionRepositoryTest {

	@Autowired
	private MetricDescriptionRepository metricDescriptionRepository;

	@Autowired
	private MetricsRepository metricsRepository;

	@Test
	@DisplayName("지표 설명을 저장하고 지표/언어로 조회한다")
	void saveAndFindByMetricAndLocale() {
		// given
		MetricsEntity metric = metricsRepository.findByMetricCode("ROA").orElseThrow();
		MetricDescriptionEntity description = MetricDescriptionEntity.create(
			metric,
			"설명",
			"해석",
			"액션",
			"ko"
		);

		// when
		metricDescriptionRepository.save(description);

		// then
		MetricDescriptionEntity found = metricDescriptionRepository.findByMetricAndLocale(metric, "ko").orElseThrow();
		assertThat(found.getDescription()).isEqualTo("설명");
		assertThat(found.getInterpretation()).isEqualTo("해석");
		assertThat(found.getActionHint()).isEqualTo("액션");
	}
}
