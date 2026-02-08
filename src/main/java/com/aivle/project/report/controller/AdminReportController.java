package com.aivle.project.report.controller;

import com.aivle.project.common.dto.ApiResponse;
import com.aivle.project.report.dto.CompanyMetricValueCommand;
import com.aivle.project.report.dto.ReportImportResult;
import com.aivle.project.report.dto.ReportMetricGroupedResponse;
import com.aivle.project.report.service.CompanyReportMetricImportService;
import com.aivle.project.report.service.CompanyReportMetricQueryService;
import com.aivle.project.report.util.ExcelIndustryMetricParser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Tag(name = "Admin Report Management", description = "관리자용 보고서 관리 API")
@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
public class AdminReportController {

    private final CompanyReportMetricImportService metricImportService;
    private final CompanyReportMetricQueryService metricQueryService;
    private final ExcelIndustryMetricParser industryMetricParser;

    @Operation(summary = "업종 상대 지표 조회", description = "특정 기업의 업종 상대 위험 지표를 분기 범위로 조회합니다.")
    @GetMapping("/companies/{stockCode}/metrics/industry-relative")
    public ApiResponse<ReportMetricGroupedResponse> getIndustryRelativeMetrics(
            @PathVariable("stockCode") String stockCode,
            @RequestParam("startYear") int startYear,
            @RequestParam("startQuarter") int startQuarter,
            @RequestParam("endYear") int endYear,
            @RequestParam("endQuarter") int endQuarter) {
        
        int fromQuarterKey = startYear * 10 + startQuarter;
        int toQuarterKey = endYear * 10 + endQuarter;
        
        ReportMetricGroupedResponse result = metricQueryService.fetchIndustryRelativeMetrics(stockCode, fromQuarterKey, toQuarterKey);
        return ApiResponse.ok(result);
    }

    @Operation(summary = "업종 상대 지표 엑셀 업로드", description = "엑셀 파일을 통해 기업별 업종 상대 위험 지표를 일괄 적재합니다.")
    @PostMapping(value = "/metrics/industry-excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ReportImportResult> uploadIndustryMetricsExcel(
            @RequestParam("year") int year,
            @RequestParam("quarter") int quarter,
            @RequestParam("file") MultipartFile file) throws IOException {
        
        int baseQuarterKey = year * 10 + quarter;
        List<CompanyMetricValueCommand> commands = industryMetricParser.parse(file);
        ReportImportResult result = metricImportService.importMetrics(baseQuarterKey, commands);
        
        return ApiResponse.ok(result);
    }
}
