package com.aivle.project.metric.entity;

import com.aivle.project.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 지표 설명 메타 엔티티.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "metric_descriptions")
public class MetricDescriptionEntity extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "metric_id", nullable = false)
	private MetricsEntity metric;

	@Column(name = "description", nullable = false, columnDefinition = "TEXT")
	private String description;

	@Column(name = "interpretation", columnDefinition = "TEXT")
	private String interpretation;

	@Column(name = "action_hint", columnDefinition = "TEXT")
	private String actionHint;

	@Column(name = "locale", nullable = false, length = 10)
	private String locale;

	/**
	 * 지표 설명 생성.
	 */
	public static MetricDescriptionEntity create(
		MetricsEntity metric,
		String description,
		String interpretation,
		String actionHint,
		String locale
	) {
		MetricDescriptionEntity entity = new MetricDescriptionEntity();
		entity.metric = metric;
		entity.description = description;
		entity.interpretation = interpretation;
		entity.actionHint = actionHint;
		entity.locale = locale;
		return entity;
	}
}
