package com.aivle.project.common.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aivle.project.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = FaviconController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class FaviconControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private UserRepository userRepository;

	@MockBean
	private JpaMetamodelMappingContext jpaMetamodelMappingContext;

	@Test
	@DisplayName("favicon 요청은 NoContent로 응답한다")
	void favicon_shouldReturnNoContent() throws Exception {
		mockMvc.perform(get("/favicon.ico"))
			.andExpect(status().isNoContent());
	}
}
