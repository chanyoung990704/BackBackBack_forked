package com.aivle.project.company.keymetric.entity;

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
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 기업 분기별 핵심 건강도 엔티티.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "company_key_metrics")
public class CompanyKeyMetricEntity extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "company_id", nullable = false)
	private CompaniesEntity company;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "quarter_id", nullable = false)
	private QuartersEntity quarter;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "report_version_id")
	private CompanyReportVersionsEntity reportVersion;

	@Column(name = "internal_health_score", precision = 6, scale = 2)
	private BigDecimal internalHealthScore;

	@Column(name = "external_health_score", precision = 6, scale = 2)
	private BigDecimal externalHealthScore;

	@Column(name = "composite_score", precision = 6, scale = 2)
	private BigDecimal compositeScore;

	@Enumerated(EnumType.STRING)
	@Column(name = "risk_level", nullable = false, length = 10)
	private CompanyKeyMetricRiskLevel riskLevel;

	@Column(name = "calculation_logic_ver", nullable = false)
	private int calculationLogicVer;

	@Column(name = "calculated_at", nullable = false)
	private LocalDateTime calculatedAt;

	@Column(name = "ai_comment", columnDefinition = "TEXT")
	private String aiComment;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "ai_sections")
	private String aiSections;

	@Column(name = "ai_model_version", length = 20)
	private String aiModelVersion;

	@Column(name = "ai_prompt_hash", length = 64)
	private String aiPromptHash;

	@Column(name = "ai_analyzed_at")
	private LocalDateTime aiAnalyzedAt;

	/**
	 * 핵심 건강도 생성.
	 */
	public static CompanyKeyMetricEntity create(
		CompaniesEntity company,
		QuartersEntity quarter,
		CompanyReportVersionsEntity reportVersion,
		BigDecimal internalHealthScore,
		BigDecimal externalHealthScore,
		BigDecimal compositeScore,
		CompanyKeyMetricRiskLevel riskLevel,
		int calculationLogicVer,
		LocalDateTime calculatedAt
	) {
		CompanyKeyMetricEntity entity = new CompanyKeyMetricEntity();
		entity.company = company;
		entity.quarter = quarter;
		entity.reportVersion = reportVersion;
		entity.internalHealthScore = internalHealthScore;
		entity.externalHealthScore = externalHealthScore;
		entity.compositeScore = compositeScore;
		entity.riskLevel = riskLevel;
		entity.calculationLogicVer = calculationLogicVer;
		entity.calculatedAt = calculatedAt;
		return entity;
	}

	/**
	 * AI 분석 메타데이터를 갱신한다.
	 */
	public void applyAiAnalysis(
		String aiComment,
		String aiSections,
		String aiModelVersion,
		String aiPromptHash,
		LocalDateTime aiAnalyzedAt
	) {
		this.aiComment = aiComment;
		this.aiSections = aiSections;
		this.aiModelVersion = aiModelVersion;
		this.aiPromptHash = aiPromptHash;
		this.aiAnalyzedAt = aiAnalyzedAt;
	}

	/**
	 * 재무건전성 점수 결과를 반영한다.
	 */
	public void applyHealthScore(
		BigDecimal internalHealthScore,
		BigDecimal compositeScore,
		CompanyKeyMetricRiskLevel riskLevel,
		int calculationLogicVer,
		LocalDateTime calculatedAt
	) {
		this.internalHealthScore = internalHealthScore;
		this.compositeScore = compositeScore;
		this.riskLevel = riskLevel;
		this.calculationLogicVer = calculationLogicVer;
		this.calculatedAt = calculatedAt;
	}

	/**
	 * 외부 건강도 점수를 반영한다.
	 */
	public void applyExternalHealthScore(BigDecimal externalHealthScore) {
		this.externalHealthScore = externalHealthScore;
	}
}
