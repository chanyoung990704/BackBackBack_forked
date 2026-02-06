package com.aivle.project.risk.dto;

/**
 * 위험도 배치 계산 대상(기업-분기-최신 보고서 버전) 조회 프로젝션.
 */
public interface RiskScoreBatchTargetProjection {

	Long getCompanyId();

	Long getQuarterId();

	Long getReportVersionId();
}
