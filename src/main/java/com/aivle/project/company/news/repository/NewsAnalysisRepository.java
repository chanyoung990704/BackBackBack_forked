package com.aivle.project.company.news.repository;

import com.aivle.project.company.news.entity.NewsAnalysisEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 뉴스 분석 세션을 조회하는 리포지토리.
 */
public interface NewsAnalysisRepository extends JpaRepository<NewsAnalysisEntity, Long> {

	/**
	 * 특정 기업의 최신 분석 조회 (analyzed_at 기준 내림차순).
	 */
	Optional<NewsAnalysisEntity> findTopByCompanyIdOrderByAnalyzedAtDesc(Long companyId);

	/**
	 * 특정 기업의 분석 이력 조회 (기간별).
	 */
	List<NewsAnalysisEntity> findByCompanyIdAndAnalyzedAtBetweenOrderByAnalyzedAtDesc(
		Long companyId, LocalDateTime start, LocalDateTime end
	);

	/**
	 * 특정 기업의 모든 분석 이력 조회.
	 */
	List<NewsAnalysisEntity> findByCompanyIdOrderByAnalyzedAtDesc(Long companyId);
}
