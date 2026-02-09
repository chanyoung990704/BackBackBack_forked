package com.aivle.project.watchlist.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.doNothing;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aivle.project.common.config.TestSecurityConfig;
import com.aivle.project.common.error.CommonException;
import com.aivle.project.watchlist.dto.WatchlistMetricValueRow;
import com.aivle.project.watchlist.dto.WatchlistMetricValuesResponse;
import com.aivle.project.watchlist.dto.WatchlistQuarterMetricValues;
import com.aivle.project.watchlist.error.WatchlistErrorCode;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class CompanyWatchlistControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private com.aivle.project.watchlist.service.CompanyWatchlistService companyWatchlistService;

	@Test
	@DisplayName("워치리스트 등록 요청이 성공하면 200을 반환한다")
	void add_shouldReturnOk() throws Exception {
		// given
		doNothing().when(companyWatchlistService).addWatchlist(any(), eq(10L), eq("메모"));

		// when & then
		mockMvc.perform(post("/api/watchlists")
				.with(jwt().jwt(jwt -> jwt.claim("userId", 1L)).authorities(new SimpleGrantedAuthority("ROLE_USER")))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "companyId": 10,
					  "note": "메모"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.error").isEmpty());
	}

	@Test
	@DisplayName("중복 등록이면 409와 WATCHLIST_DUPLICATE 에러를 반환한다")
	void add_shouldReturnConflictWhenDuplicate() throws Exception {
		// given
		willThrow(new CommonException(WatchlistErrorCode.WATCHLIST_DUPLICATE))
			.given(companyWatchlistService).addWatchlist(any(), eq(10L), eq("메모"));

		// when & then
		mockMvc.perform(post("/api/watchlists")
				.with(jwt().jwt(jwt -> jwt.claim("userId", 1L)).authorities(new SimpleGrantedAuthority("ROLE_USER")))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "companyId": 10,
					  "note": "메모"
					}
					"""))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("WATCHLIST_DUPLICATE"))
			.andExpect(jsonPath("$.error.message").value("중복저장입니다."));
	}

	@Test
	@DisplayName("워치리스트 등록 기업 조회가 성공하면 목록을 반환한다")
	void getWatchlist_shouldReturnList() throws Exception {
		// given
		java.time.LocalDateTime now = java.time.LocalDateTime.now();
		com.aivle.project.watchlist.dto.WatchlistResponse response = new com.aivle.project.watchlist.dto.WatchlistResponse(List.of(
			new com.aivle.project.watchlist.dto.WatchlistItem(1L, 10L, "기업A", "00000001", "000001", "메모A", now),
			new com.aivle.project.watchlist.dto.WatchlistItem(2L, 11L, "기업B", "00000002", "000002", "메모B", now)
		));
		given(companyWatchlistService.getWatchlist(any()))
			.willReturn(response);

		// when & then
		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/watchlists")
				.with(jwt().jwt(jwt -> jwt.claim("userId", 1L)).authorities(new SimpleGrantedAuthority("ROLE_USER"))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.items").isArray())
			.andExpect(jsonPath("$.data.items[0].corpName").value("기업A"))
			.andExpect(jsonPath("$.data.items[1].corpName").value("기업B"));
	}

	@Test
	@DisplayName("워치리스트 지표 값 조회가 성공하면 분기별 그룹 응답을 반환한다")
	void metricValues_shouldReturnGroupedResponse() throws Exception {
		// given
		WatchlistMetricValuesResponse response = new WatchlistMetricValuesResponse(List.of(
			new WatchlistQuarterMetricValues(2026, 1, List.of(
				new WatchlistMetricValueRow(1L, 10L, "기업", "000001", "ROE", "자기자본이익률", new BigDecimal("12.34"))
			))
		));
		given(companyWatchlistService.getWatchlistMetricValuesByQuarter(any(), eq(2026), eq(1)))
			.willReturn(response);

		// when & then
		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/watchlists/metric-values")
				.with(jwt().jwt(jwt -> jwt.claim("userId", 1L)).authorities(new SimpleGrantedAuthority("ROLE_USER")))
				.param("year", "2026")
				.param("quarter", "1"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.quarters[0].year").value(2026))
			.andExpect(jsonPath("$.data.quarters[0].quarter").value(1))
			.andExpect(jsonPath("$.data.quarters[0].items[0].metricCode").value("ROE"));
	}

	@Test
	@DisplayName("워치리스트 지표 값 범위 조회가 성공하면 분기별 그룹 응답을 반환한다")
	void metricValuesRange_shouldReturnGroupedResponse() throws Exception {
		// given
		WatchlistMetricValuesResponse response = new WatchlistMetricValuesResponse(List.of(
			new WatchlistQuarterMetricValues(2024, 4, List.of(
				new WatchlistMetricValueRow(1L, 10L, "기업", "000001", "ROE", "자기자본이익률", new BigDecimal("12.34"))
			)),
			new WatchlistQuarterMetricValues(2025, 1, List.of(
				new WatchlistMetricValueRow(2L, 11L, "기업2", "000002", "ROA", "총자산이익률", new BigDecimal("8.90"))
			))
		));
		given(companyWatchlistService.getWatchlistMetricValuesByQuarterRange(any(), eq(2024), eq(4), eq(2025), eq(1)))
			.willReturn(response);

		// when & then
		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/watchlists/metric-values")
				.with(jwt().jwt(jwt -> jwt.claim("userId", 1L)).authorities(new SimpleGrantedAuthority("ROLE_USER")))
				.param("fromYear", "2024")
				.param("fromQuarter", "4")
				.param("toYear", "2025")
				.param("toQuarter", "1"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.quarters[0].year").value(2024))
			.andExpect(jsonPath("$.data.quarters[1].year").value(2025));
	}
}
