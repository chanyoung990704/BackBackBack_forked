package com.aivle.project.company.controller;

import com.aivle.project.common.dto.ApiResponse;
import com.aivle.project.company.dto.AiAnalysisResponse;
import com.aivle.project.company.dto.AiReportFileResponse;
import com.aivle.project.company.service.CompanyAiService;
import com.aivle.project.file.entity.FilesEntity;
import com.aivle.project.file.storage.FileStreamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    private final FileStreamService fileStreamService;

    @GetMapping("/{companyCode}/ai-analysis")
    @Operation(summary = "기업 AI 분석 조회", description = "기업 코드로 AI 예측 분석 결과를 조회합니다. 연도와 분기를 입력하면 해당 시점의 예측치를 조회하며, 미입력 시 최신 실적 기준 다음 분기를 조회합니다.", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<AiAnalysisResponse>> getCompanyAnalysis(
        @Parameter(description = "기업 코드(Stock Code)", example = "005930")
        @PathVariable("companyCode") String companyCode,
        @Parameter(description = "연도", example = "2026")
        @RequestParam(value = "year", required = false) Integer year,
        @Parameter(description = "분기", example = "1")
        @RequestParam(value = "quarter", required = false) Integer quarter
    ) {
        AiAnalysisResponse response = companyAiService.getCompanyAnalysis(companyCode, year, quarter);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/{companyCode}/ai-report")
    @Operation(summary = "기업 AI 리포트 PDF 생성/저장", description = "AI 서버에서 PDF를 받아 파일 스토리지와 DB에 저장합니다. 연도와 분기를 입력하면 해당 보고서 버전으로 등록됩니다.", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<AiReportFileResponse>> generateCompanyAiReport(
        @Parameter(description = "기업 코드(Stock Code)", example = "005930")
        @PathVariable("companyCode") String companyCode,
        @Parameter(description = "연도", example = "2026")
        @RequestParam(value = "year", required = false) Integer year,
        @Parameter(description = "분기", example = "1")
        @RequestParam(value = "quarter", required = false) Integer quarter
    ) {
        FilesEntity file = companyAiService.generateAndSaveReport(companyCode, year, quarter);
        return ResponseEntity.ok(ApiResponse.ok(AiReportFileResponse.from(file)));
    }

    @GetMapping("/{companyCode}/ai-report/download")
    @Operation(summary = "기업 AI 리포트 PDF 다운로드", description = "특정 분기의 AI 리포트 PDF를 다운로드합니다.", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<?> downloadAiReport(
        @Parameter(description = "기업 코드(Stock Code)", example = "005930")
        @PathVariable("companyCode") String companyCode,
        @Parameter(description = "연도", example = "2026")
        @RequestParam("year") Integer year,
        @Parameter(description = "분기", example = "1")
        @RequestParam("quarter") Integer quarter
    ) {
        FilesEntity file = companyAiService.getReportFile(companyCode, year, quarter);
        return serveFile(file);
    }

//    @GetMapping("/id/{id}/ai-report/download")
//    @Operation(summary = "기업 AI 리포트 PDF 다운로드 (ID 기준)", description = "기업 ID와 특정 분기를 기준으로 AI 리포트 PDF를 다운로드합니다.", security = @SecurityRequirement(name = "bearerAuth"))
//    public ResponseEntity<?> downloadAiReportById(
//        @Parameter(description = "기업 ID", example = "1")
//        @PathVariable("id") Long id,
//        @Parameter(description = "연도", example = "2026")
//        @RequestParam("year") Integer year,
//        @Parameter(description = "분기", example = "1")
//        @RequestParam("quarter") Integer quarter
//    ) {
//        FilesEntity file = companyAiService.getReportFileById(id, year, quarter);
//        return serveFile(file);
//    }

    private ResponseEntity<?> serveFile(FilesEntity file) {
        InputStream stream = fileStreamService.openStream(file);
        String contentType = file.getContentType() != null ? file.getContentType() : MediaType.APPLICATION_PDF_VALUE;
        String encodedFilename = URLEncoder.encode(file.getOriginalFilename(), StandardCharsets.UTF_8)
            .replace("+", "%20");
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_TYPE, contentType)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFilename + "\"")
            .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
            .contentLength(file.getFileSize())
            .body(new InputStreamResource(stream));
    }
}
