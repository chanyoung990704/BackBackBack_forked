package com.aivle.project.common.dto;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * 공통 페이지 응답 DTO.
 */
public record PageResponse<T>(
	List<T> content,
	int page,
	int size,
	long totalElements,
	int totalPages,
	boolean last
) {

	public static <T> PageResponse<T> of(Page<T> page) {
		return new PageResponse<>(
			page.getContent(),
			page.getNumber() + 1,
			page.getSize(),
			page.getTotalElements(),
			page.getTotalPages(),
			page.isLast()
		);
	}
}
