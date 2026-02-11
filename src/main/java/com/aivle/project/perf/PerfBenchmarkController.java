package com.aivle.project.perf;

import com.aivle.project.common.dto.ApiResponse;
import com.aivle.project.company.entity.CompaniesEntity;
import com.aivle.project.company.insight.service.CompanyInsightService;
import com.aivle.project.company.news.service.NewsService;
import com.aivle.project.company.reportanalysis.service.ReportAnalysisService;
import com.aivle.project.company.repository.CompaniesRepository;
import com.aivle.project.company.service.CompanyAiService;
import io.swagger.v3.oas.annotations.Hidden;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Virtual Thread 벤치마크용 perf 전용 API.
 */
@Hidden
@RestController
@Profile("perf")
@RequiredArgsConstructor
@RequestMapping("/api/perf/benchmark")
public class PerfBenchmarkController {

	private final CompaniesRepository companiesRepository;
	private final NewsService newsService;
	private final ReportAnalysisService reportAnalysisService;
	private final CompanyInsightService companyInsightService;
	private final CompanyAiService companyAiService;

	@GetMapping("/fixture")
	public ResponseEntity<ApiResponse<Map<String, Object>>> fixture() {
		CompaniesEntity company = companiesRepository.findByStockCode(PerfDataInitializer.PERF_STOCK_CODE)
			.orElseThrow(() -> new IllegalStateException("perf 기본 기업 데이터가 없습니다."));
		return ResponseEntity.ok(ApiResponse.ok(Map.of(
			"companyId", company.getId(),
			"stockCode", company.getStockCode()
		)));
	}

	@PostMapping("/news-sync/{stockCode}")
	public ResponseEntity<ApiResponse<Map<String, Object>>> newsSync(@PathVariable String stockCode) {
		long startedAt = System.nanoTime();
		newsService.fetchAndStoreNews(stockCode);
		return ResponseEntity.ok(ApiResponse.ok(createResult("news-sync", startedAt)));
	}

	@PostMapping("/report-sync/{stockCode}")
	public ResponseEntity<ApiResponse<Map<String, Object>>> reportSync(@PathVariable String stockCode) {
		long startedAt = System.nanoTime();
		reportAnalysisService.fetchAndStoreReport(stockCode);
		return ResponseEntity.ok(ApiResponse.ok(createResult("report-sync", startedAt)));
	}

	@GetMapping("/insight-refresh/{companyId}")
	public ResponseEntity<ApiResponse<Map<String, Object>>> insightRefresh(@PathVariable Long companyId) {
		long startedAt = System.nanoTime();
		companyInsightService.getInsights(companyId, 0, 10, 0, 1, true);
		return ResponseEntity.ok(ApiResponse.ok(createResult("insight-refresh", startedAt)));
	}

	@PostMapping("/ai-report/{companyId}")
	public ResponseEntity<ApiResponse<Map<String, Object>>> aiReport(
		@PathVariable Long companyId,
		@RequestParam(required = false) Integer year,
		@RequestParam(required = false) Integer quarter
	) {
		long startedAt = System.nanoTime();
		companyAiService.generateAndSaveReport(companyId, year, quarter);
		return ResponseEntity.ok(ApiResponse.ok(createResult("ai-report", startedAt)));
	}

	private Map<String, Object> createResult(String benchmarkName, long startedAt) {
		long elapsedMs = Math.round((System.nanoTime() - startedAt) / 1_000_000d);
		return Map.of(
			"benchmark", benchmarkName,
			"elapsedMs", elapsedMs
		);
	}
}
