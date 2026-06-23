package com.aivle.project.company.watchlist.dto;

import java.util.List;

public record WatchlistResponse(
	List<WatchlistItem> items
) {
}
