package com.aivle.project.report.controller;

import com.aivle.project.common.dto.ApiResponse;
import com.aivle.project.common.error.CommonErrorCode;
import com.aivle.project.common.error.CommonException;
import com.aivle.project.report.dto.CompanyMetricValueCommand;
import com.aivle.project.report.dto.ReportImportResult;
import com.aivle.project.report.dto.ReportPredictRequest;
import com.aivle.project.report.dto.ReportPredictResult;
import com.aivle.project.report.dto.ReportPublishResult;
import com.aivle.project.report.dto.ReportMetricPublishRequest;
import com.aivle.project.report.importer.ExcelMetricParser;
import com.aivle.project.report.service.CompanyReportMetricImportService;
import com.aivle.project.report.service.CompanyReportMetricPredictService;
import com.aivle.project.report.service.CompanyReportMetricPublishService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;
import jakarta.validation.Valid;
import org.springframework.web.multipart.MultipartFile;

/**
 * 보고서 지표 엑셀 업로드 API.
 */
@Slf4j
@Tag(name = "보고서 지표", description = "기업 보고서 지표 업로드")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/reports/metrics")
public class ReportMetricImportController {

	private final ExcelMetricParser excelMetricParser;
	private final CompanyReportMetricImportService companyReportMetricImportService;
	private final CompanyReportMetricPredictService companyReportMetricPredictService;
	private final CompanyReportMetricPublishService companyReportMetricPublishService;

	@PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(summary = "보고서 지표 엑셀 업로드", description = "관리자용 엑셀 업로드 API")
	public ResponseEntity<ApiResponse<ReportImportResult>> importMetrics(
		@RequestParam("quarterKey") int quarterKey,
		@RequestParam("file") MultipartFile file
	) {
		if (file == null || file.isEmpty()) {
			throw new CommonException(CommonErrorCode.COMMON_400);
		}

		try {
			List<CompanyMetricValueCommand> commands = excelMetricParser.parse(file);
			ReportImportResult result = companyReportMetricImportService.importMetrics(quarterKey, commands);
			return ResponseEntity.ok(ApiResponse.ok(result));
		} catch (IllegalArgumentException | IOException ex) {
			log.info("보고서 지표 업로드 실패: {}", ex.getMessage());
			throw new CommonException(CommonErrorCode.COMMON_400);
		}
	}

	@PostMapping(value = "/predict", consumes = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "보고서 지표 예측값 적재", description = "관리자용 예측값 적재 API")
	public ResponseEntity<ApiResponse<ReportPredictResult>> importPredictedMetrics(
		@Valid @RequestBody ReportPredictRequest request
	) {
		ReportPredictResult result = companyReportMetricPredictService.importPredictedMetrics(request);
		return ResponseEntity.ok(ApiResponse.ok(result));
	}

	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "보고서 지표 수동 적재", description = "관리자용 지표 수동 적재 API")
	public ResponseEntity<ApiResponse<ReportPublishResult>> publishMetrics(
		@Valid @RequestBody ReportMetricPublishRequest request
	) {
		ReportPublishResult result = companyReportMetricPublishService.publishMetrics(
			request.stockCode(),
			request.quarterKey(),
			request.valueType(),
			request.metrics()
		);
		return new ResponseEntity<>(ApiResponse.ok(result), HttpStatus.OK);
	}
}
