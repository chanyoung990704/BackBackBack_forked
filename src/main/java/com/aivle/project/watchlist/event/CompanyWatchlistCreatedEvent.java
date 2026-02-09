package com.aivle.project.watchlist.event;

/**
 * 워치리스트 등록 완료 이벤트.
 */
public record CompanyWatchlistCreatedEvent(Long userId, Long companyId) {
}
