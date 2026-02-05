package com.aivle.project.risk.repository;

import com.aivle.project.risk.entity.RiskScoreSummaryEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 위험도 요약 조회/저장 리포지토리.
 */
public interface RiskScoreSummaryRepository extends JpaRepository<RiskScoreSummaryEntity, Long> {

	Optional<RiskScoreSummaryEntity> findByCompanyIdAndQuarterIdAndReportVersionId(
		Long companyId,
		Long quarterId,
		Long reportVersionId
	);
}
