package com.aivle.project.company.reportanalysis.repository;

import com.aivle.project.company.reportanalysis.entity.ReportAnalysisEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportAnalysisRepository extends JpaRepository<ReportAnalysisEntity, Long> {

	Optional<ReportAnalysisEntity> findTopByCompanyIdOrderByAnalyzedAtDesc(Long companyId);
}
