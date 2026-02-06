package com.aivle.project.company.reportanalysis.entity;

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
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * report_contents 테이블에 매핑되는 사업보고서 요약 항목 엔티티.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "report_contents")
public class ReportContentEntity extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "report_analysis_id", nullable = false)
	private ReportAnalysisEntity reportAnalysis;

	@Column(name = "title", nullable = false, length = 500)
	private String title;

	@Column(name = "summary", columnDefinition = "TEXT")
	private String summary;

	@Column(name = "score", precision = 10, scale = 6)
	private BigDecimal score;

	@Column(name = "published_at")
	private LocalDateTime publishedAt;

	@Column(name = "link", length = 2000)
	private String link;

	@Column(name = "sentiment", length = 10)
	private String sentiment;

	/**
	 * 사업보고서 요약 항목 엔티티 생성.
	 */
	@Builder
	public static ReportContentEntity create(
		ReportAnalysisEntity reportAnalysis,
		String title,
		String summary,
		BigDecimal score,
		LocalDateTime publishedAt,
		String link,
		String sentiment
	) {
		ReportContentEntity entity = new ReportContentEntity();
		entity.reportAnalysis = reportAnalysis;
		entity.title = title;
		entity.summary = summary;
		entity.score = score;
		entity.publishedAt = publishedAt;
		entity.link = link;
		entity.sentiment = sentiment;
		return entity;
	}
}
