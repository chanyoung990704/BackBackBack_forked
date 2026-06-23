package com.aivle.project.company.report.dto;

import com.aivle.project.company.metric.entity.MetricValueType;
import com.aivle.project.company.report.entity.SignalColor;
import java.math.BigDecimal;

/**
 * 기업 개요 지표 조회 프로젝션.
 */
public interface CompanyOverviewMetricRowProjection {

	String getMetricCode();

	String getMetricNameKo();

	String getUnit();

	BigDecimal getMetricValue();

	MetricValueType getValueType();

	int getQuarterKey();

	SignalColor getSignalColor();

	String getDescription();

	String getInterpretation();

	String getActionHint();
}
