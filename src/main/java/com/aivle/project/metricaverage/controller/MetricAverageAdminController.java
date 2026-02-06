package com.aivle.project.metricaverage.controller;

import com.aivle.project.common.dto.ApiResponse;
import com.aivle.project.metricaverage.service.MetricAverageBatchSaveResult;
import com.aivle.project.metricaverage.service.MetricAverageBatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * metric_averages 관리자 배치 실행 API.
 */
@Tag(name = "Metric Average Admin", description = "분기별 전체 기업 지표 통계 저장 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/metric-averages")
@SecurityRequirement(name = "bearerAuth")
public class MetricAverageAdminController {

	private final MetricAverageBatchService metricAverageBatchService;

	@PostMapping("/initialize")
	@Operation(summary = "전체 분기 통계 초기 저장", description = "모든 분기를 순회하며 metric_averages를 없을 때만 저장합니다.")
	public ResponseEntity<ApiResponse<MetricAverageBatchSaveResult>> initialize() {
		MetricAverageBatchSaveResult result = metricAverageBatchService.calculateAndInsertMissingAllQuarters(
			"MANUAL",
			UUID.randomUUID().toString()
		);
		return ResponseEntity.ok(ApiResponse.ok(result));
	}
}
