package com.aivle.project.metricaverage.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aivle.project.common.config.TestSecurityConfig;
import com.aivle.project.metricaverage.service.MetricAverageBatchSaveResult;
import com.aivle.project.metricaverage.service.MetricAverageBatchService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class MetricAverageAdminControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private MetricAverageBatchService metricAverageBatchService;

	@Test
	@DisplayName("전체 분기 metric_averages 초기 저장 API가 성공한다")
	void initialize_shouldReturnBatchResult() throws Exception {
		// given
		given(metricAverageBatchService.calculateAndInsertMissingAllQuarters(anyString(), anyString()))
			.willReturn(new MetricAverageBatchSaveResult(10, 120, 80, "MANUAL", "exec-1"));

		// when & then
		mockMvc.perform(post("/api/admin/metric-averages/initialize")
				.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.processedQuarterCount").value(10))
			.andExpect(jsonPath("$.data.insertedCount").value(120))
			.andExpect(jsonPath("$.data.skippedCount").value(80))
			.andExpect(jsonPath("$.data.triggerType").value("MANUAL"))
			.andExpect(jsonPath("$.data.executionId").value("exec-1"));
	}
}
