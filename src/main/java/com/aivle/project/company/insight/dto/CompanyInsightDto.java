package com.aivle.project.company.insight.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

/**
 * 기업 인사이트 응답 DTO.
 */
@Getter
@Builder
public class CompanyInsightDto {

	private Long id;
	private CompanyInsightType type;
	private String title;
	private String body;
	private String content;
	private String source;

	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
	private LocalDateTime publishedAt;

	private String url;
}
