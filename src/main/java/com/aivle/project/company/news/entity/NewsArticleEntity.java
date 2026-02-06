package com.aivle.project.company.news.entity;

import com.aivle.project.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * news_articles 테이블에 매핑되는 뉴스 기사 엔티티.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "news_articles")
public class NewsArticleEntity extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "news_analysis_id", nullable = false)
	private NewsAnalysisEntity newsAnalysis;

	@Column(name = "title", nullable = false, length = 500)
	private String title;

	@Column(name = "summary", columnDefinition = "TEXT")
	private String summary;

	@Column(name = "score", precision = 10, scale = 6)
	private BigDecimal score;

	@Column(name = "published_at")
	private LocalDateTime publishedAt;

	@Column(name = "link", nullable = false, length = 2000)
	private String link;

	@Column(name = "sentiment", nullable = false, length = 10)
	private String sentiment;

	/**
	 * 뉴스 기사 엔티티 생성.
	 */
	@Builder
	public static NewsArticleEntity create(
		NewsAnalysisEntity newsAnalysis,
		String title,
		String summary,
		BigDecimal score,
		LocalDateTime publishedAt,
		String link,
		String sentiment
	) {
		NewsArticleEntity entity = new NewsArticleEntity();
		entity.newsAnalysis = newsAnalysis;
		entity.title = title;
		entity.summary = summary;
		entity.score = score;
		entity.publishedAt = publishedAt;
		entity.link = link;
		entity.sentiment = sentiment != null ? sentiment : "NEU";
		return entity;
	}
}
