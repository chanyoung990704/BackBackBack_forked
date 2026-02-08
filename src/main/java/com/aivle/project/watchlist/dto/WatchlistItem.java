package com.aivle.project.watchlist.dto;

import java.time.LocalDateTime;

public record WatchlistItem(
	Long id,
	Long companyId,
	String corpName,
	String corpCode,
	String stockCode,
	String note,
	LocalDateTime createdAt
) {
}
