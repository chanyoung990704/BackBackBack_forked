package com.aivle.project.company.watchlist.dto;

import jakarta.validation.constraints.NotNull;

public record WatchlistAddRequest(
	@NotNull(message = "companyId는 필수입니다.")
	Long companyId,
	String note
) {
}
