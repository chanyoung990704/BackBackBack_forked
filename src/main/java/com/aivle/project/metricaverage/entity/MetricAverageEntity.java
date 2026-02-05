package com.aivle.project.metricaverage.entity;

import com.aivle.project.common.entity.BaseEntity;
import com.aivle.project.metric.entity.MetricsEntity;
import com.aivle.project.quarter.entity.QuartersEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 분기별 지표 집계 엔티티.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "metric_averages")
public class MetricAverageEntity extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "quarter_id", nullable = false)
	private QuartersEntity quarter;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "metric_id", nullable = false)
	private MetricsEntity metric;

	@Column(name = "avg_value", precision = 20, scale = 4)
	private BigDecimal avgValue;

	@Column(name = "median_value", precision = 20, scale = 4)
	private BigDecimal medianValue;

	@Column(name = "min_value", precision = 20, scale = 4)
	private BigDecimal minValue;

	@Column(name = "max_value", precision = 20, scale = 4)
	private BigDecimal maxValue;

	@Column(name = "stddev_value", precision = 20, scale = 4)
	private BigDecimal stddevValue;

	@Column(name = "company_count", nullable = false)
	private int companyCount;

	@Column(name = "calculated_at", nullable = false)
	private LocalDateTime calculatedAt;

	@Column(name = "data_source_version", nullable = false)
	private int dataSourceVersion;

	public static MetricAverageEntity create(
		QuartersEntity quarter,
		MetricsEntity metric,
		BigDecimal avgValue,
		BigDecimal medianValue,
		BigDecimal minValue,
		BigDecimal maxValue,
		BigDecimal stddevValue,
		int companyCount,
		LocalDateTime calculatedAt,
		int dataSourceVersion
	) {
		MetricAverageEntity entity = new MetricAverageEntity();
		entity.quarter = quarter;
		entity.metric = metric;
		entity.avgValue = avgValue;
		entity.medianValue = medianValue;
		entity.minValue = minValue;
		entity.maxValue = maxValue;
		entity.stddevValue = stddevValue;
		entity.companyCount = companyCount;
		entity.calculatedAt = calculatedAt;
		entity.dataSourceVersion = dataSourceVersion;
		return entity;
	}

	public void refresh(
		BigDecimal avgValue,
		BigDecimal medianValue,
		BigDecimal minValue,
		BigDecimal maxValue,
		BigDecimal stddevValue,
		int companyCount,
		LocalDateTime calculatedAt,
		int dataSourceVersion
	) {
		this.avgValue = avgValue;
		this.medianValue = medianValue;
		this.minValue = minValue;
		this.maxValue = maxValue;
		this.stddevValue = stddevValue;
		this.companyCount = companyCount;
		this.calculatedAt = calculatedAt;
		this.dataSourceVersion = dataSourceVersion;
	}
}
