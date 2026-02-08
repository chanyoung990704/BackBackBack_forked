package com.aivle.project.report.repository;

import com.aivle.project.metric.entity.MetricValueType;
import com.aivle.project.report.entity.CompanyReportMetricValuesEntity;
import com.aivle.project.report.entity.CompanyReportVersionsEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 보고서 지표 값 조회/저장 리포지토리.
 */
public interface CompanyReportMetricValuesRepository extends JpaRepository<CompanyReportMetricValuesEntity, Long>, CompanyReportMetricValuesRepositoryCustom {

	boolean existsByReportVersionAndValueTypeAndMetricValueIsNotNull(
		CompanyReportVersionsEntity reportVersion,
		MetricValueType valueType
	);

	@Query("""
		select max(q.quarterKey)
		from CompanyReportMetricValuesEntity v
		join v.quarter q
		join v.reportVersion rv
		join rv.companyReport cr
		join cr.company c
		where c.stockCode = :stockCode
		  and v.valueType = com.aivle.project.metric.entity.MetricValueType.ACTUAL
		""")
	Optional<Integer> findMaxActualQuarterKeyByStockCode(@Param("stockCode") String stockCode);
}