package com.aivle.project.company.report.quarter.support;

/**
 * 연도-분기 값 객체.
 */
public record YearQuarter(int year, int quarter) {

	public int toQuarterKey() {
		return year * 10 + quarter;
	}
}
