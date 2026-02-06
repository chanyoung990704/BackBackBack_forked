package com.aivle.project.company.news.repository;

import com.aivle.project.company.news.entity.NewsArticleEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 뉴스 기사를 조회하는 리포지토리.
 */
public interface NewsArticleRepository extends JpaRepository<NewsArticleEntity, Long> {

	/**
	 * 특정 분석의 모든 뉴스 조회.
	 */
	List<NewsArticleEntity> findByNewsAnalysisIdOrderByPublishedAtDesc(Long newsAnalysisId);

	/**
	 * 특정 분석의 뉴스 페이징 조회.
	 */
	Page<NewsArticleEntity> findByNewsAnalysisIdOrderByPublishedAtDesc(Long newsAnalysisId, Pageable pageable);

	boolean existsByNewsAnalysisId(Long newsAnalysisId);

	/**
	 * 특정 기간의 뉴스 조회.
	 */
	List<NewsArticleEntity> findByNewsAnalysisCompanyIdAndPublishedAtBetweenOrderByPublishedAtDesc(
		Long companyId, LocalDateTime start, LocalDateTime end
	);

	/**
	 * 특정 감성 분류의 뉴스 조회.
	 */
	List<NewsArticleEntity> findByNewsAnalysisIdAndSentimentOrderByPublishedAtDesc(
		Long newsAnalysisId, String sentiment
	);
}
