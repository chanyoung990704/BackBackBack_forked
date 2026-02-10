package com.aivle.project.dashboard.dto;

/**
 * KPI 카드 DTO.
 */
public record KpiCardDto(
	String key,
	String title,
	Object value,
	String unit,
	KpiTone tone,
	KpiDeltaDto delta,
	KpiBadgeDto badge,
	KpiTooltipDto tooltip
) {

	public enum KpiTone {
		DEFAULT,
		GOOD,
		WARN,
		RISK
	}

	public record KpiDeltaDto(
		double value,
		String unit,
		Direction direction,
		String label
	) {
		public enum Direction {
			UP,
			DOWN,
			FLAT
		}
	}

	public record KpiBadgeDto(
		String label,
		String subLabel
	) {
	}

	public record KpiTooltipDto(
		String description,
		String interpretation,
		String actionHint
	) {
	}
}
