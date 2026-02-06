package com.aivle.project.company.reportanalysis.repository;

import com.aivle.project.company.reportanalysis.entity.ReportContentEntity;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportContentRepository extends JpaRepository<ReportContentEntity, Long> {

	List<ReportContentEntity> findByReportAnalysisIdOrderByPublishedAtDesc(Long reportAnalysisId);

	Page<ReportContentEntity> findByReportAnalysisIdOrderByPublishedAtDesc(Long reportAnalysisId, Pageable pageable);

	boolean existsByReportAnalysisId(Long reportAnalysisId);
}
