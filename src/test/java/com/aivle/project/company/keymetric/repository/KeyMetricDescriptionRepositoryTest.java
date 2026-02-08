package com.aivle.project.company.keymetric.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.aivle.project.common.config.QuerydslConfig;
import com.aivle.project.company.keymetric.entity.KeyMetricDescriptionEntity;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import(QuerydslConfig.class)
class KeyMetricDescriptionRepositoryTest {

	@Autowired
	private KeyMetricDescriptionRepository keyMetricDescriptionRepository;

	@Test
	@DisplayName("핵심 건강도 설명을 코드로 조회한다")
	void findByMetricCode() {
		// given
		KeyMetricDescriptionEntity entity = KeyMetricDescriptionEntity.create(
			"INTERNAL_HEALTH",
			"내부 건강도",
			"점",
			"설명",
			"해석",
			"액션",
			new BigDecimal("0.60"),
			new BigDecimal("70.00"),
			new BigDecimal("40.00"),
			true
		);
		keyMetricDescriptionRepository.save(entity);

		// when
		KeyMetricDescriptionEntity found = keyMetricDescriptionRepository.findByMetricCode("INTERNAL_HEALTH").orElseThrow();

		// then
		assertThat(found.getMetricName()).isEqualTo("내부 건강도");
		assertThat(found.getUnit()).isEqualTo("점");
		assertThat(found.isActive()).isTrue();
	}
}
