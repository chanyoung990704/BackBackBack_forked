package com.aivle.project.watchlist.dto;

import java.util.List;

public record WatchlistResponse(
	List<WatchlistItem> items
) {
}
