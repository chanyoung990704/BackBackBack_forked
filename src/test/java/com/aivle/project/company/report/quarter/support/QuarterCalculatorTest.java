package com.aivle.project.company.report.quarter.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class QuarterCalculatorTest {

	@Test
	@DisplayName("quarter_key를 연도-분기로 파싱한다")
	void parseQuarterKey() {
		// given
		int quarterKey = 20253;

		// when
		YearQuarter result = QuarterCalculator.parseQuarterKey(quarterKey);

		// then
		assertThat(result.year()).isEqualTo(2025);
		assertThat(result.quarter()).isEqualTo(3);
		assertThat(result.toQuarterKey()).isEqualTo(20253);
	}

	@Test
	@DisplayName("유효하지 않은 quarter_key는 예외를 던진다")
	void parseQuarterKey_invalid() {
		// given
		int invalid = 20255;

		// when & then
		assertThatThrownBy(() -> QuarterCalculator.parseQuarterKey(invalid))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("분기 오프셋을 계산한다")
	void offsetQuarter() {
		// given
		YearQuarter current = new YearQuarter(2025, 3);

		// when
		YearQuarter qMinus1 = QuarterCalculator.offset(current, -1);
		YearQuarter qMinus2 = QuarterCalculator.offset(current, -2);
		YearQuarter qMinus3 = QuarterCalculator.offset(current, -3);
		YearQuarter qPlus1 = QuarterCalculator.offset(current, 1);

		// then
		assertThat(qMinus1).isEqualTo(new YearQuarter(2025, 2));
		assertThat(qMinus2).isEqualTo(new YearQuarter(2025, 1));
		assertThat(qMinus3).isEqualTo(new YearQuarter(2024, 4));
		assertThat(qPlus1).isEqualTo(new YearQuarter(2025, 4));
	}

	@Test
	@DisplayName("분기 시작/종료일을 계산한다")
	void computeDateRange() {
		// given
		YearQuarter q3 = new YearQuarter(2025, 3);
		YearQuarter q4 = new YearQuarter(2024, 4);

		// when
		LocalDate startQ3 = QuarterCalculator.startDate(q3);
		LocalDate endQ3 = QuarterCalculator.endDate(q3);
		LocalDate startQ4 = QuarterCalculator.startDate(q4);
		LocalDate endQ4 = QuarterCalculator.endDate(q4);

		// then
		assertThat(startQ3).isEqualTo(LocalDate.of(2025, 7, 1));
		assertThat(endQ3).isEqualTo(LocalDate.of(2025, 9, 30));
		assertThat(startQ4).isEqualTo(LocalDate.of(2024, 10, 1));
		assertThat(endQ4).isEqualTo(LocalDate.of(2024, 12, 31));
	}
}
