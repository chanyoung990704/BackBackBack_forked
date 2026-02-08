package com.aivle.project.watchlist.repository;

import com.aivle.project.company.entity.QCompaniesEntity;
import com.aivle.project.metric.entity.MetricValueType;
import com.aivle.project.metric.entity.QMetricsEntity;
import com.aivle.project.metricaverage.entity.QMetricAverageEntity;
import com.aivle.project.quarter.entity.QQuartersEntity;
import com.aivle.project.report.entity.QCompanyReportMetricValuesEntity;
import com.aivle.project.report.entity.QCompanyReportVersionsEntity;
import com.aivle.project.report.entity.QCompanyReportsEntity;
import com.aivle.project.risk.entity.QRiskScoreSummaryEntity;
import com.aivle.project.risk.entity.RiskLevel;
import com.aivle.project.watchlist.dto.WatchlistDashboardMetricRow;
import com.aivle.project.watchlist.dto.WatchlistDashboardRiskRow;
import com.aivle.project.watchlist.dto.WatchlistMetricAverageRow;
import com.aivle.project.watchlist.entity.QCompanyWatchlistEntity;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CompanyWatchlistRepositoryImpl implements CompanyWatchlistRepositoryCustom {

	private final JPAQueryFactory queryFactory;

	@Override
	public List<WatchlistDashboardMetricRow> findDashboardMetrics(
		Long userId,
		short year,
		byte quarter,
		MetricValueType valueType,
		Collection<String> metricCodes,
		boolean metricCodesEmpty
	) {
		QCompanyWatchlistEntity cw = QCompanyWatchlistEntity.companyWatchlistEntity;
		QCompaniesEntity c = QCompaniesEntity.companiesEntity;
		QCompanyReportsEntity cr = QCompanyReportsEntity.companyReportsEntity;
		QQuartersEntity q = QQuartersEntity.quartersEntity;
		QCompanyReportVersionsEntity crv = QCompanyReportVersionsEntity.companyReportVersionsEntity;
		QCompanyReportMetricValuesEntity crmv = QCompanyReportMetricValuesEntity.companyReportMetricValuesEntity;
		QMetricsEntity m = QMetricsEntity.metricsEntity;
		QMetricAverageEntity ma = QMetricAverageEntity.metricAverageEntity;

		QCompanyReportVersionsEntity crv2 = new QCompanyReportVersionsEntity("crv2");
		QCompanyReportMetricValuesEntity v2 = new QCompanyReportMetricValuesEntity("v2");

		return queryFactory
			.select(Projections.constructor(WatchlistDashboardMetricRow.class,
				cw.id,
				c.id,
				c.corpName,
				c.corpCode,
				m.metricCode,
				m.metricNameKo,
				crmv.metricValue,
				ma.avgValue,
				ma.companyCount
			))
			.from(cw)
			.join(cw.company, c)
			.join(cr).on(cr.company.eq(c))
			.join(cr.quarter, q)
			.join(crv).on(crv.companyReport.eq(cr))
			.join(crmv).on(crmv.reportVersion.eq(crv).and(crmv.quarter.eq(q)))
			.join(crmv.metric, m)
			.leftJoin(ma).on(ma.quarter.eq(q).and(ma.metric.eq(m)))
			.where(
				cw.user.id.eq(userId),
				cw.deletedAt.isNull(),
				q.year.eq(year),
				q.quarter.eq(quarter),
				m.isRiskIndicator.isFalse(),
				crmv.valueType.eq(valueType),
				crmv.metricValue.isNotNull(),
				crv.versionNo.eq(
					JPAExpressions.select(crv2.versionNo.max())
						.from(crv2)
						.where(crv2.companyReport.eq(cr),
							JPAExpressions.selectOne()
								.from(v2)
								.where(v2.reportVersion.eq(crv2),
									v2.valueType.eq(valueType),
									v2.metricValue.isNotNull()
								).exists()
						)
				),
				metricCodesExpression(m, metricCodes, metricCodesEmpty)
			)
			.orderBy(c.corpName.asc(), m.metricCode.asc())
			.fetch();
	}

	@Override
	public List<WatchlistDashboardRiskRow> findDashboardRisks(
		Long userId,
		Long quarterId,
		RiskLevel riskLevel
	) {
		QCompanyWatchlistEntity cw = QCompanyWatchlistEntity.companyWatchlistEntity;
		QCompaniesEntity c = QCompaniesEntity.companiesEntity;
		QRiskScoreSummaryEntity rss = QRiskScoreSummaryEntity.riskScoreSummaryEntity;

		return queryFactory
			.select(Projections.constructor(WatchlistDashboardRiskRow.class,
				cw.id,
				c.id,
				c.corpName,
				rss.riskScore,
				rss.riskLevel,
				rss.riskMetricsAvg,
				rss.updatedAt
			))
			.from(cw)
			.join(cw.company, c)
			.join(rss).on(rss.company.eq(c))
			.where(
				cw.user.id.eq(userId),
				cw.deletedAt.isNull(),
				rss.quarter.id.eq(quarterId),
				riskLevelEq(rss, riskLevel)
			)
			.orderBy(rss.riskScore.desc())
			.fetch();
	}

	@Override
	public List<WatchlistMetricValueProjection> findWatchlistMetricValues(
		Long userId,
		short year,
		byte quarter,
		MetricValueType valueType
	) {
		QCompanyWatchlistEntity cw = QCompanyWatchlistEntity.companyWatchlistEntity;
		QCompaniesEntity c = QCompaniesEntity.companiesEntity;
		QCompanyReportsEntity cr = QCompanyReportsEntity.companyReportsEntity;
		QQuartersEntity q = QQuartersEntity.quartersEntity;
		QCompanyReportVersionsEntity crv = QCompanyReportVersionsEntity.companyReportVersionsEntity;
		QCompanyReportMetricValuesEntity crmv = QCompanyReportMetricValuesEntity.companyReportMetricValuesEntity;
		QMetricsEntity m = QMetricsEntity.metricsEntity;

		QCompanyReportVersionsEntity crv2 = new QCompanyReportVersionsEntity("crv2");
		QCompanyReportMetricValuesEntity v2 = new QCompanyReportMetricValuesEntity("v2");

		return queryFactory
			.select(Projections.constructor(WatchlistMetricValueDto.class,
				cw.id,
				c.id,
				c.corpName,
				c.corpCode,
				m.metricCode,
				m.metricNameKo,
				crmv.metricValue,
				q.year.castToNum(Integer.class),
				q.quarter.castToNum(Integer.class)
			))
			.from(cw)
			.join(cw.company, c)
			.join(cr).on(cr.company.eq(c))
			.join(cr.quarter, q)
			.join(crv).on(crv.companyReport.eq(cr))
			.join(crmv).on(crmv.reportVersion.eq(crv).and(crmv.quarter.eq(q)))
			.join(crmv.metric, m)
			.where(
				cw.user.id.eq(userId),
				cw.deletedAt.isNull(),
				q.year.eq(year),
				q.quarter.eq(quarter),
				crmv.valueType.eq(valueType),
				crmv.metricValue.isNotNull(),
				crv.versionNo.eq(
					JPAExpressions.select(crv2.versionNo.max())
						.from(crv2)
						.where(crv2.companyReport.eq(cr),
							JPAExpressions.selectOne()
								.from(v2)
								.where(v2.reportVersion.eq(crv2),
									v2.valueType.eq(valueType),
									v2.metricValue.isNotNull()
								).exists()
						)
				)
			)
			.orderBy(c.corpName.asc(), m.metricCode.asc())
			.fetch()
			.stream()
			.map(dto -> (WatchlistMetricValueProjection) dto)
			.toList();
	}

	@Override
	public List<WatchlistMetricValueProjection> findWatchlistMetricValuesInRange(
		Long userId,
		short fromYear,
		byte fromQuarter,
		short toYear,
		byte toQuarter,
		MetricValueType valueType
	) {
		QCompanyWatchlistEntity cw = QCompanyWatchlistEntity.companyWatchlistEntity;
		QCompaniesEntity c = QCompaniesEntity.companiesEntity;
		QCompanyReportsEntity cr = QCompanyReportsEntity.companyReportsEntity;
		QQuartersEntity q = QQuartersEntity.quartersEntity;
		QCompanyReportVersionsEntity crv = QCompanyReportVersionsEntity.companyReportVersionsEntity;
		QCompanyReportMetricValuesEntity crmv = QCompanyReportMetricValuesEntity.companyReportMetricValuesEntity;
		QMetricsEntity m = QMetricsEntity.metricsEntity;

		QCompanyReportVersionsEntity crv2 = new QCompanyReportVersionsEntity("crv2");
		QCompanyReportMetricValuesEntity v2 = new QCompanyReportMetricValuesEntity("v2");

		return queryFactory
			.select(Projections.constructor(WatchlistMetricValueDto.class,
				cw.id,
				c.id,
				c.corpName,
				c.corpCode,
				m.metricCode,
				m.metricNameKo,
				crmv.metricValue,
				q.year.castToNum(Integer.class),
				q.quarter.castToNum(Integer.class)
			))
			.from(cw)
			.join(cw.company, c)
			.join(cr).on(cr.company.eq(c))
			.join(cr.quarter, q)
			.join(crv).on(crv.companyReport.eq(cr))
			.join(crmv).on(crmv.reportVersion.eq(crv).and(crmv.quarter.eq(q)))
			.join(crmv.metric, m)
			.where(
				cw.user.id.eq(userId),
				cw.deletedAt.isNull(),
				q.year.gt(fromYear).or(q.year.eq(fromYear).and(q.quarter.goe(fromQuarter))),
				q.year.lt(toYear).or(q.year.eq(toYear).and(q.quarter.loe(toQuarter))),
				crmv.valueType.eq(valueType),
				crmv.metricValue.isNotNull(),
				crv.versionNo.eq(
					JPAExpressions.select(crv2.versionNo.max())
						.from(crv2)
						.where(crv2.companyReport.eq(cr),
							JPAExpressions.selectOne()
								.from(v2)
								.where(v2.reportVersion.eq(crv2),
									v2.valueType.eq(valueType),
									v2.metricValue.isNotNull()
								).exists()
						)
				)
			)
			.orderBy(q.year.asc(), q.quarter.asc(), c.corpName.asc(), m.metricCode.asc())
			.fetch()
			.stream()
			.map(dto -> (WatchlistMetricValueProjection) dto)
			.toList();
	}

	@Override
	public List<WatchlistMetricAverageRow> findWatchlistMetricAverages(
		Long userId,
		short year,
		byte quarter,
		MetricValueType valueType,
		Collection<String> metricCodes,
		boolean metricCodesEmpty
	) {
		QCompanyWatchlistEntity cw = QCompanyWatchlistEntity.companyWatchlistEntity;
		QCompaniesEntity c = QCompaniesEntity.companiesEntity;
		QCompanyReportsEntity cr = QCompanyReportsEntity.companyReportsEntity;
		QQuartersEntity q = QQuartersEntity.quartersEntity;
		QCompanyReportVersionsEntity crv = QCompanyReportVersionsEntity.companyReportVersionsEntity;
		QCompanyReportMetricValuesEntity crmv = QCompanyReportMetricValuesEntity.companyReportMetricValuesEntity;
		QMetricsEntity m = QMetricsEntity.metricsEntity;

		QCompanyReportVersionsEntity crv2 = new QCompanyReportVersionsEntity("crv2");
		QCompanyReportMetricValuesEntity v2 = new QCompanyReportMetricValuesEntity("v2");

		return queryFactory
			.select(Projections.constructor(WatchlistMetricAverageRow.class,
				m.metricCode,
				m.metricNameKo,
				Expressions.numberTemplate(BigDecimal.class, "cast({0} as bigdecimal)", crmv.metricValue.avg()),
				c.id.countDistinct()
			))
			.from(cw)
			.join(cw.company, c)
			.join(cr).on(cr.company.eq(c))
			.join(cr.quarter, q)
			.join(crv).on(crv.companyReport.eq(cr))
			.join(crmv).on(crmv.reportVersion.eq(crv).and(crmv.quarter.eq(q)))
			.join(crmv.metric, m)
			.where(
				cw.user.id.eq(userId),
				cw.deletedAt.isNull(),
				q.year.eq(year),
				q.quarter.eq(quarter),
				crmv.valueType.eq(valueType),
				m.isRiskIndicator.isFalse(),
				crmv.metricValue.isNotNull(),
				crv.versionNo.eq(
					JPAExpressions.select(crv2.versionNo.max())
						.from(crv2)
						.where(crv2.companyReport.eq(cr),
							JPAExpressions.selectOne()
								.from(v2)
								.where(v2.reportVersion.eq(crv2),
									v2.valueType.eq(valueType),
									v2.metricValue.isNotNull()
								).exists()
						)
				),
				metricCodesExpression(m, metricCodes, metricCodesEmpty)
			)
			.groupBy(m.id, m.metricCode, m.metricNameKo)
			.orderBy(m.metricCode.asc())
			.fetch();
	}

	private BooleanExpression metricCodesExpression(QMetricsEntity m, Collection<String> metricCodes, boolean metricCodesEmpty) {
		if (metricCodesEmpty) {
			return null;
		}
		return m.metricCode.in(metricCodes);
	}

	private BooleanExpression riskLevelEq(QRiskScoreSummaryEntity rss, RiskLevel riskLevel) {
		if (riskLevel == null) {
			return null;
		}
		return rss.riskLevel.eq(riskLevel);
	}

	@Getter
	@RequiredArgsConstructor
	public static class WatchlistMetricValueDto implements WatchlistMetricValueProjection {
		private final Long watchlistId;
		private final Long companyId;
		private final String corpName;
		private final String corpCode;
		private final String metricCode;
		private final String metricNameKo;
		private final BigDecimal metricValue;
		private final Integer year;
		private final Integer quarter;
	}
}
