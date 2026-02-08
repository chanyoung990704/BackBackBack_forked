package com.aivle.project.company.keymetric.entity;

import com.aivle.project.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 핵심 건강도 지표 정의 엔티티.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "key_metric_descriptions")
public class KeyMetricDescriptionEntity extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "metric_code", nullable = false, length = 30)
	private String metricCode;

	@Column(name = "metric_name", nullable = false, length = 100)
	private String metricName;

	@Column(name = "unit", length = 20)
	private String unit;

	@Column(name = "description", nullable = false, columnDefinition = "TEXT")
	private String description;

	@Column(name = "interpretation", columnDefinition = "TEXT")
	private String interpretation;

	@Column(name = "action_hint", columnDefinition = "TEXT")
	private String actionHint;

	@Column(name = "weight", precision = 3, scale = 2)
	private BigDecimal weight;

	@Column(name = "threshold_safe", precision = 6, scale = 2)
	private BigDecimal thresholdSafe;

	@Column(name = "threshold_warn", precision = 6, scale = 2)
	private BigDecimal thresholdWarn;

	@Column(name = "is_active", nullable = false)
	private boolean isActive;

	/**
	 * 핵심 건강도 설명 생성.
	 */
	public static KeyMetricDescriptionEntity create(
		String metricCode,
		String metricName,
		String unit,
		String description,
		String interpretation,
		String actionHint,
		BigDecimal weight,
		BigDecimal thresholdSafe,
		BigDecimal thresholdWarn,
		boolean isActive
	) {
		KeyMetricDescriptionEntity entity = new KeyMetricDescriptionEntity();
		entity.metricCode = metricCode;
		entity.metricName = metricName;
		entity.unit = unit;
		entity.description = description;
		entity.interpretation = interpretation;
		entity.actionHint = actionHint;
		entity.weight = weight;
		entity.thresholdSafe = thresholdSafe;
		entity.thresholdWarn = thresholdWarn;
		entity.isActive = isActive;
		return entity;
	}
}
