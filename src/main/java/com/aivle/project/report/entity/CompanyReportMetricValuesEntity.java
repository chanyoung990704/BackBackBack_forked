package com.aivle.project.report.entity;

import com.aivle.project.common.entity.BaseEntity;
import com.aivle.project.metric.entity.MetricValueType;
import com.aivle.project.metric.entity.MetricsEntity;
import com.aivle.project.quarter.entity.QuartersEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * company_report_metric_values 테이블에 매핑되는 지표 값 엔티티.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "company_report_metric_values")
public class CompanyReportMetricValuesEntity extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "report_version_id", nullable = false)
	private CompanyReportVersionsEntity reportVersion;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "metric_id", nullable = false)
	private MetricsEntity metric;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "quarter_id", nullable = false)
	private QuartersEntity quarter;

	@Column(name = "metric_value", precision = 20, scale = 4)
	private BigDecimal metricValue;

	@Enumerated(EnumType.STRING)
	@Column(name = "value_type", nullable = false, length = 20)
	private MetricValueType valueType = MetricValueType.ACTUAL;

	@Enumerated(EnumType.STRING)
	@Column(name = "signal_color", length = 10)
	private SignalColor signalColor;

	@Column(name = "color_rationale", length = 255)
	private String colorRationale;

	@Column(name = "benchmark_value", precision = 20, scale = 4)
	private BigDecimal benchmarkValue;

	/**
	 * 지표 값 생성.
	 */
	public static CompanyReportMetricValuesEntity create(
		CompanyReportVersionsEntity reportVersion,
		MetricsEntity metric,
		QuartersEntity quarter,
		BigDecimal metricValue,
		MetricValueType valueType
	) {
		CompanyReportMetricValuesEntity value = new CompanyReportMetricValuesEntity();
		value.reportVersion = reportVersion;
		value.metric = metric;
		value.quarter = quarter;
		value.metricValue = metricValue;
		value.valueType = valueType;
		return value;
	}

	/**
	 * 지표 신호등 정보를 반영한다.
	 */
	public void applySignal(SignalColor signalColor, String colorRationale, BigDecimal benchmarkValue) {
		this.signalColor = signalColor;
		this.colorRationale = colorRationale;
		this.benchmarkValue = benchmarkValue;
	}
}
