package com.aivle.project.metric.entity;

import com.aivle.project.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * metrics 테이블에 매핑되는 지표 엔티티.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "metrics")
public class MetricsEntity extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "metric_code", nullable = false, length = 50)
	private String metricCode;

	@Column(name = "metric_name_ko", nullable = false, length = 100)
	private String metricNameKo;

	@Column(name = "metric_name_en", length = 100)
	private String metricNameEn;

	@Column(name = "unit", length = 20)
	private String unit;

	@Column(name = "is_risk_indicator", nullable = false)
	private boolean isRiskIndicator;

	public static MetricsEntity create(String metricCode, String metricNameKo, String metricNameEn, boolean isRiskIndicator) {
		MetricsEntity entity = new MetricsEntity();
		entity.metricCode = metricCode;
		entity.metricNameKo = metricNameKo;
		entity.metricNameEn = metricNameEn;
		entity.isRiskIndicator = isRiskIndicator;
		return entity;
	}

}
