package com.aivle.project.company.report.quarter.support;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * 분기 계산 유틸.
 */
public final class QuarterCalculator {

	private QuarterCalculator() {
	}

	public static YearQuarter parseQuarterKey(int quarterKey) {
		int year = quarterKey / 10;
		int quarter = quarterKey % 10;
		if (quarter < 1 || quarter > 4) {
			throw new IllegalArgumentException("quarter_key가 유효하지 않습니다: " + quarterKey);
		}
		return new YearQuarter(year, quarter);
	}

	public static YearQuarter offset(YearQuarter current, int offset) {
		int newYear = current.year();
		int newQuarter = current.quarter() + offset;
		while (newQuarter <= 0) {
			newQuarter += 4;
			newYear--;
		}
		while (newQuarter > 4) {
			newQuarter -= 4;
			newYear++;
		}
		return new YearQuarter(newYear, newQuarter);
	}

	public static LocalDate startDate(YearQuarter quarter) {
		int startMonth = (quarter.quarter() - 1) * 3 + 1;
		return LocalDate.of(quarter.year(), startMonth, 1);
	}

	public static LocalDate endDate(YearQuarter quarter) {
		int endMonth = quarter.quarter() * 3;
		return YearMonth.of(quarter.year(), endMonth).atEndOfMonth();
	}
}
