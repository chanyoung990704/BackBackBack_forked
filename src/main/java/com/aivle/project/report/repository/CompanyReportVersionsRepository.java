package com.aivle.project.report.repository;

import com.aivle.project.report.entity.CompanyReportVersionsEntity;
import com.aivle.project.report.entity.CompanyReportsEntity;
import com.aivle.project.risk.dto.RiskScoreBatchTargetProjection;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * 보고서 버전 조회/저장 리포지토리.
 */
public interface CompanyReportVersionsRepository extends JpaRepository<CompanyReportVersionsEntity, Long> {

	Optional<CompanyReportVersionsEntity> findTopByCompanyReportOrderByVersionNoDesc(CompanyReportsEntity companyReport);

	Optional<CompanyReportVersionsEntity> findTopByCompanyReportAndPublishedFalseOrderByVersionNoDesc(
		CompanyReportsEntity companyReport
	);

	@Query("""
		select cr.company.id as companyId,
			cr.quarter.id as quarterId,
			rv.id as reportVersionId
		from CompanyReportVersionsEntity rv
		join rv.companyReport cr
		where rv.versionNo = (
			select max(rv2.versionNo)
			from CompanyReportVersionsEntity rv2
			where rv2.companyReport = cr
		)
		order by cr.quarter.id, cr.company.id
		""")
	Page<RiskScoreBatchTargetProjection> findLatestRiskScoreTargets(Pageable pageable);

	@Query("""
		select rv
		from CompanyReportVersionsEntity rv
		join rv.companyReport cr
		join cr.company c
		join cr.quarter q
		where c.stockCode = :stockCode
		  and q.quarterKey = :quarterKey
		  and rv.pdfFile is not null
		order by rv.versionNo desc
		""")
	java.util.List<CompanyReportVersionsEntity> findLatestWithPdf(
		@org.springframework.data.repository.query.Param("stockCode") String stockCode,
		@org.springframework.data.repository.query.Param("quarterKey") int quarterKey,
		Pageable pageable
	);
}
