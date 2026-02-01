package com.aivle.project.category.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aivle.project.category.dto.CategorySummaryResponse;
import com.aivle.project.category.entity.CategoriesEntity;
import com.aivle.project.category.mapper.CategoryMapper;
import com.aivle.project.category.repository.CategoriesRepository;
import com.aivle.project.user.repository.UserRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = CategoryController.class)
@AutoConfigureMockMvc(addFilters = false)
class CategoryControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private CategoriesRepository categoriesRepository;

	@MockBean
	private CategoryMapper categoryMapper;

	@MockBean
	private UserRepository userRepository;

	@MockBean
	private JpaMetamodelMappingContext jpaMetamodelMappingContext;

	@Test
	@DisplayName("카테고리 목록 조회가 성공한다")
	void list_shouldReturnCategories() throws Exception {
		CategoriesEntity category = CategoriesEntity.create("공지", "공지사항", 1, true);
		CategorySummaryResponse response = new CategorySummaryResponse(1L, "공지", "공지사항", 1, true);

		given(categoriesRepository.findAllByDeletedAtIsNullOrderBySortOrderAscIdAsc())
			.willReturn(List.of(category));
		given(categoryMapper.toSummaryResponse(any(CategoriesEntity.class)))
			.willReturn(response);

		mockMvc.perform(get("/categories"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data[0].name").value("공지"));
	}
}
