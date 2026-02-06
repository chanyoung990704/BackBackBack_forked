package com.aivle.project.watchlist.repository;

import com.aivle.project.metric.entity.MetricValueType;
import com.aivle.project.risk.entity.RiskLevel;
import com.aivle.project.watchlist.entity.CompanyWatchlistEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CompanyWatchlistRepository extends JpaRepository<CompanyWatchlistEntity, Long> {

	Optional<CompanyWatchlistEntity> findByUserIdAndCompanyId(Long userId, Long companyId);

	@Query("""
		select cw from CompanyWatchlistEntity cw
		where cw.user.id = :userId and cw.deletedAt is null
		order by cw.id desc
		""")
	List<CompanyWatchlistEntity> findActiveByUserId(@Param("userId") Long userId);

	@Query("""
		select cw.id as watchlistId,
			c.id as companyId,
			c.corpName as corpName,
			c.corpCode as corpCode,
			m.metricCode as metricCode,
			m.metricNameKo as metricNameKo,
			crmv.metricValue as metricValue,
			ma.avgValue as marketAvg,
			ma.companyCount as marketN
		from CompanyWatchlistEntity cw
		join cw.company c
		join CompanyReportsEntity cr on cr.company = c
		join cr.quarter q
		join CompanyReportVersionsEntity crv on crv.companyReport = cr
		join CompanyReportMetricValuesEntity crmv on crmv.reportVersion = crv and crmv.quarter = q
		join crmv.metric m
		left join MetricAverageEntity ma on ma.quarter = q and ma.metric = m
		where cw.user.id = :userId
			and cw.deletedAt is null
			and q.year = :year
			and q.quarter = :quarter
			and m.isRiskIndicator = false
			and crmv.valueType = :valueType
			and crmv.metricValue is not null
			and crv.versionNo = (
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
			and (:metricCodesEmpty = true or m.metricCode in :metricCodes)
		order by c.corpName, m.metricCode
		""")
	List<WatchlistDashboardMetricProjection> findDashboardMetrics(
		@Param("userId") Long userId,
		@Param("year") short year,
		@Param("quarter") byte quarter,
		@Param("valueType") MetricValueType valueType,
		@Param("metricCodes") Collection<String> metricCodes,
		@Param("metricCodesEmpty") boolean metricCodesEmpty
	);

	@Query("""
		select cw.id as watchlistId,
			c.id as companyId,
			c.corpName as corpName,
			rss.riskScore as riskScore,
			rss.riskLevel as riskLevel,
			rss.riskMetricsAvg as riskMetricsAvg,
			rss.updatedAt as lastRefreshedAt
		from CompanyWatchlistEntity cw
		join cw.company c
		join RiskScoreSummaryEntity rss on rss.company = c
		where cw.user.id = :userId
			and cw.deletedAt is null
			and rss.quarter.id = :quarterId
			and (:riskLevel is null or rss.riskLevel = :riskLevel)
		order by rss.riskScore desc
		""")
	List<WatchlistDashboardRiskProjection> findDashboardRisks(
		@Param("userId") Long userId,
		@Param("quarterId") Long quarterId,
		@Param("riskLevel") RiskLevel riskLevel
	);

	@Query("""
		select cw.id as watchlistId,
			c.id as companyId,
			c.corpName as corpName,
			c.corpCode as corpCode,
			m.metricCode as metricCode,
			m.metricNameKo as metricNameKo,
			crmv.metricValue as metricValue,
			q.year as year,
			q.quarter as quarter
		from CompanyWatchlistEntity cw
		join cw.company c
		join CompanyReportsEntity cr on cr.company = c
		join cr.quarter q
		join CompanyReportVersionsEntity crv on crv.companyReport = cr
		join CompanyReportMetricValuesEntity crmv on crmv.reportVersion = crv and crmv.quarter = q
		join crmv.metric m
		where cw.user.id = :userId
			and cw.deletedAt is null
			and q.year = :year
			and q.quarter = :quarter
			and crmv.valueType = :valueType
			and crmv.metricValue is not null
			and crv.versionNo = (
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
		order by c.corpName, m.metricCode
		""")
	List<WatchlistMetricValueProjection> findWatchlistMetricValues(
		@Param("userId") Long userId,
		@Param("year") short year,
		@Param("quarter") byte quarter,
		@Param("valueType") MetricValueType valueType
	);

	@Query("""
		select cw.id as watchlistId,
			c.id as companyId,
			c.corpName as corpName,
			c.corpCode as corpCode,
			m.metricCode as metricCode,
			m.metricNameKo as metricNameKo,
			crmv.metricValue as metricValue,
			q.year as year,
			q.quarter as quarter
		from CompanyWatchlistEntity cw
		join cw.company c
		join CompanyReportsEntity cr on cr.company = c
		join cr.quarter q
		join CompanyReportVersionsEntity crv on crv.companyReport = cr
		join CompanyReportMetricValuesEntity crmv on crmv.reportVersion = crv and crmv.quarter = q
		join crmv.metric m
		where cw.user.id = :userId
			and cw.deletedAt is null
			and (
				q.year > :fromYear
				or (q.year = :fromYear and q.quarter >= :fromQuarter)
			)
			and (
				q.year < :toYear
				or (q.year = :toYear and q.quarter <= :toQuarter)
			)
			and crmv.valueType = :valueType
			and crmv.metricValue is not null
			and crv.versionNo = (
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
		order by q.year, q.quarter, c.corpName, m.metricCode
		""")
	List<WatchlistMetricValueProjection> findWatchlistMetricValuesInRange(
		@Param("userId") Long userId,
		@Param("fromYear") short fromYear,
		@Param("fromQuarter") byte fromQuarter,
		@Param("toYear") short toYear,
		@Param("toQuarter") byte toQuarter,
		@Param("valueType") MetricValueType valueType
	);

	@Query("""
		select m.metricCode as metricCode,
			m.metricNameKo as metricNameKo,
			avg(crmv.metricValue) as avgValue,
			count(distinct c.id) as sampleCompanyCount
		from CompanyWatchlistEntity cw
		join cw.company c
		join CompanyReportsEntity cr on cr.company = c
		join cr.quarter q
		join CompanyReportVersionsEntity crv on crv.companyReport = cr
		join CompanyReportMetricValuesEntity crmv on crmv.reportVersion = crv and crmv.quarter = q
		join crmv.metric m
		where cw.user.id = :userId
			and cw.deletedAt is null
			and q.year = :year
			and q.quarter = :quarter
			and crmv.valueType = :valueType
			and m.isRiskIndicator = false
			and crmv.metricValue is not null
			and crv.versionNo = (
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
			and (:metricCodesEmpty = true or m.metricCode in :metricCodes)
		group by m.id, m.metricCode, m.metricNameKo
		order by m.metricCode
		""")
	List<WatchlistMetricAverageProjection> findWatchlistMetricAverages(
		@Param("userId") Long userId,
		@Param("year") short year,
		@Param("quarter") byte quarter,
		@Param("valueType") MetricValueType valueType,
		@Param("metricCodes") Collection<String> metricCodes,
		@Param("metricCodesEmpty") boolean metricCodesEmpty
	);
}
