package com.aivle.project.company.news.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;

/**
 * 뉴스 기사 DTO.
 */
@Schema(description = "뉴스 기사")
public record NewsArticleDto(
    @Schema(description = "뉴스 기사 ID", example = "1")
    Long id,

    @Schema(description = "뉴스 제목")
    String title,

    @Schema(description = "뉴스 요약")
    String summary,

    @Schema(description = "감성 점수", example = "0.0007")
    Double score,

    @Schema(description = "뉴스 발행 시각 (UTC)", example = "2026-01-30T09:34:00+09:00")
    OffsetDateTime publishedAt,

    @Schema(description = "뉴스 URL")
    String link,

    @Schema(description = "감성 분류 (POS/NEU/NEG)", example = "NEU")
    String sentiment,

    @Schema(description = "생성 시각 (UTC)", example = "2026-02-05T12:35:41.369155Z")
    OffsetDateTime createdAt
) {

    /**
     * 뉴스 기사 엔티티에서 DTO로 변환.
     */
    public static NewsArticleDto from(com.aivle.project.company.news.entity.NewsArticleEntity entity) {
        return new NewsArticleDto(
            entity.getId(),
            entity.getTitle(),
            entity.getSummary(),
            entity.getScore() != null ? entity.getScore().doubleValue() : null,
            entity.getPublishedAt() != null ? entity.getPublishedAt().atZone(ZoneOffset.UTC).toOffsetDateTime() : null,
            entity.getLink(),
            entity.getSentiment(),
            entity.getCreatedAt().atZone(ZoneOffset.UTC).toOffsetDateTime()
        );
    }

    /**
     * AI 서버 응답의 뉴스 아이템에서 DTO로 변환.
     */
    public static NewsArticleDto from(NewsItemResponse item) {
        return new NewsArticleDto(
            null, // ID는 DB 저장 시 생성됨
            item.title(),
            item.summary(),
            item.score() != null ? item.score() : null,
            parseDate(item.date()),
            item.link(),
            item.sentiment(),
            OffsetDateTime.now(ZoneOffset.UTC)
        );
    }

    private static OffsetDateTime parseDate(String rawDate) {
        if (rawDate == null || rawDate.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(rawDate);
        } catch (DateTimeParseException ignored) {
            return LocalDateTime.parse(rawDate).atOffset(ZoneOffset.UTC);
        }
    }
}
