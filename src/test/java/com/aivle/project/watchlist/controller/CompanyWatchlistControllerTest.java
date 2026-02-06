package com.aivle.project.watchlist.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.doNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aivle.project.common.config.TestSecurityConfig;
import com.aivle.project.common.error.CommonException;
import com.aivle.project.watchlist.error.WatchlistErrorCode;
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
				.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
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
				.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
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
}
