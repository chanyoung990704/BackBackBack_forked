package com.aivle.project.company.news.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.aivle.project.company.news.dto.NewsApiResponse;
import com.aivle.project.company.reportanalysis.dto.ReportApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NewsClientTest {

	@Test
	@DisplayName("mock 모드에서 뉴스 수집 응답을 반환한다")
	void fetchNews_shouldReturnMockResponseWhenMockModeEnabled() {
		// given
		NewsClient newsClient = new NewsClient("http://localhost:8080", true, 0);

		// when
		NewsApiResponse response = newsClient.fetchNews("900001", "PERF_MOCK_COMPANY");

		// then
		assertThat(response).isNotNull();
		assertThat(response.companyName()).isEqualTo("PERF_MOCK_COMPANY");
		assertThat(response.news()).isNotEmpty();
	}

	@Test
	@DisplayName("mock 모드에서 사업보고서 수집 응답을 반환한다")
	void fetchReport_shouldReturnMockResponseWhenMockModeEnabled() {
		// given
		NewsClient newsClient = new NewsClient("http://localhost:8080", true, 0);

		// when
		ReportApiResponse response = newsClient.fetchReport("900001");

		// then
		assertThat(response).isNotNull();
		assertThat(response.news()).isNotEmpty();
		assertThat(response.averageScore()).isNotNull();
	}
}
