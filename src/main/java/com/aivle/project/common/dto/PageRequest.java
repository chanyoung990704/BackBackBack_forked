package com.aivle.project.common.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Sort;

/**
 * 공통 페이지 요청 DTO.
 */
@Getter
@Setter
public class PageRequest {

	private int page = 1;
	private int size = 10;
	private String sortBy = "id";
	private Sort.Direction direction = Sort.Direction.DESC;

	public org.springframework.data.domain.PageRequest toPageable() {
		return org.springframework.data.domain.PageRequest.of(page - 1, size, direction, sortBy);
	}
}
