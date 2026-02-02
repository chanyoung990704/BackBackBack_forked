package com.aivle.project.category.controller;

import com.aivle.project.category.dto.CategorySummaryResponse;
import com.aivle.project.category.mapper.CategoryMapper;
import com.aivle.project.category.repository.CategoriesRepository;
import com.aivle.project.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 카테고리 조회 API.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/categories")
@Tag(name = "카테고리", description = "카테고리 조회 API")
public class CategoryController {

	private final CategoriesRepository categoriesRepository;
	private final CategoryMapper categoryMapper;

	@GetMapping
	@Operation(summary = "카테고리 조회", description = "카테고리 목록을 조회합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
	})
	public ApiResponse<List<CategorySummaryResponse>> list() {
		List<CategorySummaryResponse> response = categoriesRepository
			.findAllByDeletedAtIsNullOrderBySortOrderAscIdAsc()
			.stream()
			.map(categoryMapper::toSummaryResponse)
			.toList();
		return ApiResponse.ok(response);
	}
}
