package com.aivle.project.company.controller;

import com.aivle.project.common.dto.ApiResponse;
import com.aivle.project.company.dto.AiAnalysisResponse;
import com.aivle.project.company.dto.AiReportFileResponse;
import com.aivle.project.company.service.CompanyAiService;
import com.aivle.project.file.entity.FilesEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 기업 AI 분석 조회 API.
 */
@Tag(name = "기업", description = "기업 AI 분석")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/companies")
public class CompanyAiController {

    private final CompanyAiService companyAiService;

    @GetMapping("/{companyCode}/ai-analysis")
    @Operation(summary = "기업 AI 분석 조회", description = "기업 코드로 AI 예측 분석 결과를 조회합니다.")
    public ResponseEntity<ApiResponse<AiAnalysisResponse>> getCompanyAnalysis(
        @Parameter(description = "기업 코드", example = "005930")
        @PathVariable("companyCode") String companyCode
    ) {
        AiAnalysisResponse response = companyAiService.getCompanyAnalysis(companyCode);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/{companyCode}/ai-report")
    @Operation(summary = "기업 AI 리포트 PDF 생성/저장", description = "AI 서버에서 PDF를 받아 파일 스토리지와 DB에 저장합니다.")
    public ResponseEntity<ApiResponse<AiReportFileResponse>> generateCompanyAiReport(
        @Parameter(description = "기업 코드", example = "005930")
        @PathVariable("companyCode") String companyCode
    ) {
        FilesEntity file = companyAiService.generateAndSaveReport(companyCode);
        return ResponseEntity.ok(ApiResponse.ok(AiReportFileResponse.from(file)));
    }
}
