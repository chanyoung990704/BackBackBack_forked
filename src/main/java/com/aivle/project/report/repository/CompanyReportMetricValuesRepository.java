package com.aivle.project.report.repository;

import com.aivle.project.report.dto.ReportMetricRowProjection;
import com.aivle.project.report.dto.ReportPredictMetricRowProjection;
import com.aivle.project.report.dto.MetricValueSampleProjection;
import com.aivle.project.report.entity.CompanyReportMetricValuesEntity;
import com.aivle.project.report.entity.CompanyReportVersionsEntity;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.aivle.project.metric.entity.MetricValueType;

/**
 * 보고서 지표 값 조회/저장 리포지토리.
 */
public interface CompanyReportMetricValuesRepository extends JpaRepository<CompanyReportMetricValuesEntity, Long> {

	boolean existsByReportVersionAndValueTypeAndMetricValueIsNotNull(
		CompanyReportVersionsEntity reportVersion,
		MetricValueType valueType
	);

	@Query("""
		select c.corpName as corpName,
			c.stockCode as stockCode,
			m.metricCode as metricCode,
			m.metricNameKo as metricNameKo,
			v.metricValue as metricValue,
			v.valueType as valueType,
			q.quarterKey as quarterKey,
			rv.versionNo as versionNo,
			rv.generatedAt as generatedAt
		from CompanyReportMetricValuesEntity v
		join v.reportVersion rv
		join rv.companyReport cr
		join cr.company c
		join v.metric m
		join v.quarter q
		where c.stockCode = :stockCode
			and q.quarterKey between :fromQuarterKey and :toQuarterKey
			and rv.versionNo = (
				select max(rv2.versionNo)
				from CompanyReportVersionsEntity rv2
				where rv2.companyReport = cr
					and exists (
						select 1
						from CompanyReportMetricValuesEntity v2
						where v2.reportVersion = rv2
							and v2.valueType = com.aivle.project.metric.entity.MetricValueType.ACTUAL
							and v2.metricValue is not null
					)
			)
		order by q.quarterKey, m.metricCode
		""")
	List<ReportMetricRowProjection> findLatestMetricsByStockCodeAndQuarterRange(
		@Param("stockCode") String stockCode,
		@Param("fromQuarterKey") int fromQuarterKey,
		@Param("toQuarterKey") int toQuarterKey
	);

	@Query("""
		select c.corpName as corpName,
			c.stockCode as stockCode,
			m.metricCode as metricCode,
			m.metricNameKo as metricNameKo,
			v.metricValue as metricValue,
			v.valueType as valueType,
			q.quarterKey as quarterKey,
			rv.versionNo as versionNo,
			rv.generatedAt as generatedAt,
			f.id as pdfFileId,
			f.originalFilename as pdfFileName,
			f.contentType as pdfContentType
		from CompanyReportMetricValuesEntity v
		join v.reportVersion rv
		join rv.companyReport cr
		join cr.company c
		join v.metric m
		join v.quarter q
		left join rv.pdfFile f
		where c.stockCode = :stockCode
			and cr.quarter.quarterKey = :quarterKey
			and v.valueType = :valueType
			and rv.versionNo = (
				select max(rv2.versionNo)
				from CompanyReportVersionsEntity rv2
				where rv2.companyReport = cr
					and exists (
						select 1
						from CompanyReportMetricValuesEntity v2
						where v2.reportVersion = rv2
							and v2.valueType = :valueType
							and v2.metricValue is not null
					)
			)
		order by m.metricCode
		""")
	List<ReportPredictMetricRowProjection> findLatestMetricsByStockCodeAndQuarterKeyAndType(
		@Param("stockCode") String stockCode,
		@Param("quarterKey") int quarterKey,
		@Param("valueType") MetricValueType valueType
	);

	@Query("""
		select v.metricValue
		from CompanyReportMetricValuesEntity v
		join v.reportVersion rv
		join rv.companyReport cr
		join v.metric m
		where cr.company.id = :companyId
			and cr.quarter.id = :quarterId
			and rv.id = :reportVersionId
			and v.quarter.id = :quarterId
			and v.valueType = :valueType
			and m.isRiskIndicator = true
			and v.metricValue is not null
		""")
	List<BigDecimal> findRiskMetricValuesByCompanyQuarterAndVersion(
		@Param("companyId") Long companyId,
		@Param("quarterId") Long quarterId,
		@Param("reportVersionId") Long reportVersionId,
		@Param("valueType") MetricValueType valueType
	);

	@Query("""
		select m.id as metricId,
			v.metricValue as metricValue
		from CompanyReportMetricValuesEntity v
		join v.reportVersion rv
		join rv.companyReport cr
		join v.metric m
		where cr.quarter.id = :quarterId
			and v.quarter.id = :quarterId
			and v.valueType = :valueType
			and m.isRiskIndicator = false
			and v.metricValue is not null
			and rv.versionNo = (
				select max(rv2.versionNo)
				from CompanyReportVersionsEntity rv2
				where rv2.companyReport = cr
					and exists (
						select 1
						from CompanyReportMetricValuesEntity v2
						where v2.reportVersion = rv2
							and v2.valueType = :valueType
							and v2.metricValue is not null
					)
			)
		""")
	List<MetricValueSampleProjection> findNonRiskActualMetricSamplesByQuarterId(
		@Param("quarterId") Long quarterId,
		@Param("valueType") MetricValueType valueType
	);
}
