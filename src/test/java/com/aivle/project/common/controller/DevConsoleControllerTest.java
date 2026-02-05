package com.aivle.project.common.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import com.aivle.project.auth.service.TurnstileService;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = DevConsoleController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles({"test", "dev"})
class DevConsoleControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private TurnstileService turnstileService;

	@MockBean
	private CategoriesRepository categoriesRepository;

	@MockBean
	private CategoryMapper categoryMapper;

	@MockBean
	private UserRepository userRepository;

	@MockBean
	private JpaMetamodelMappingContext jpaMetamodelMappingContext;

	@Test
	@DisplayName("개발용 카테고리 조회가 성공한다")
	void listCategories_shouldReturnCategories() throws Exception {
		CategoriesEntity category = CategoriesEntity.create("공지", "공지사항", 1, true);
		CategorySummaryResponse response = new CategorySummaryResponse(1L, "공지", "공지사항", 1, true);

		given(categoriesRepository.findAllByDeletedAtIsNullOrderBySortOrderAscIdAsc())
			.willReturn(List.of(category));
		given(categoryMapper.toSummaryResponse(any(CategoriesEntity.class)))
			.willReturn(response);

		mockMvc.perform(get("/dev/categories"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data[0].name").value("공지"));
	}

	@Test
	@DisplayName("개발용 API 콘솔 페이지에 기업 AI 분석 테스트 UI가 노출된다")
	void console_shouldContainCompanyAiSection() throws Exception {
		mockMvc.perform(get("/dev/console"))
			.andExpect(status().isOk())
			.andExpect(content().string(org.hamcrest.Matchers.containsString("ai-analysis-query")));
	}
}
