package com.aivle.project.risk.entity;

import com.aivle.project.common.entity.BaseEntity;
import com.aivle.project.company.entity.CompaniesEntity;
import com.aivle.project.quarter.entity.QuartersEntity;
import com.aivle.project.report.entity.CompanyReportVersionsEntity;
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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 기업-분기-보고서 버전 기준 위험도 요약 엔티티.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "risk_score_summaries")
public class RiskScoreSummaryEntity extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "company_id", nullable = false)
	private CompaniesEntity company;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "quarter_id", nullable = false)
	private QuartersEntity quarter;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "report_version_id", nullable = false)
	private CompanyReportVersionsEntity reportVersion;

	@Column(name = "risk_score", precision = 6, scale = 2)
	private BigDecimal riskScore;

	@Enumerated(EnumType.STRING)
	@Column(name = "risk_level", nullable = false, length = 20)
	private RiskLevel riskLevel;

	@Column(name = "risk_metrics_count", nullable = false)
	private int riskMetricsCount;

	@Column(name = "risk_metrics_avg", precision = 6, scale = 2)
	private BigDecimal riskMetricsAvg;

	@Column(name = "calculated_at", nullable = false)
	private LocalDateTime calculatedAt;

	/**
	 * 위험도 요약 생성.
	 */
	public static RiskScoreSummaryEntity create(
		CompaniesEntity company,
		QuartersEntity quarter,
		CompanyReportVersionsEntity reportVersion,
		BigDecimal riskScore,
		RiskLevel riskLevel,
		int riskMetricsCount,
		BigDecimal riskMetricsAvg,
		LocalDateTime calculatedAt
	) {
		RiskScoreSummaryEntity summary = new RiskScoreSummaryEntity();
		summary.company = company;
		summary.quarter = quarter;
		summary.reportVersion = reportVersion;
		summary.riskScore = riskScore;
		summary.riskLevel = riskLevel;
		summary.riskMetricsCount = riskMetricsCount;
		summary.riskMetricsAvg = riskMetricsAvg;
		summary.calculatedAt = calculatedAt;
		return summary;
	}

	/**
	 * 재계산 결과로 값을 갱신한다.
	 */
	public void refresh(BigDecimal riskScore, RiskLevel riskLevel, int riskMetricsCount, BigDecimal riskMetricsAvg, LocalDateTime calculatedAt) {
		this.riskScore = riskScore;
		this.riskLevel = riskLevel;
		this.riskMetricsCount = riskMetricsCount;
		this.riskMetricsAvg = riskMetricsAvg;
		this.calculatedAt = calculatedAt;
	}
}
