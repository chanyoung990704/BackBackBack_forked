package com.aivle.project.report.controller;

import com.aivle.project.common.dto.ApiResponse;
import com.aivle.project.common.error.CommonErrorCode;
import com.aivle.project.common.error.CommonException;
import com.aivle.project.report.dto.ReportPdfPublishResult;
import com.aivle.project.report.service.CompanyReportPdfPublishService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 보고서 PDF 업로드 API.
 */
//@Tag(name = "보고서 PDF", description = "보고서 PDF 단일 업로드")
//@RestController
//@RequiredArgsConstructor
//@RequestMapping("/api/admin/reports")
//@SecurityRequirement(name = "bearerAuth")
//public class ReportPdfPublishController {
//
//	private final CompanyReportPdfPublishService companyReportPdfPublishService;
//
//	@PostMapping(value = "/pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
//	@Operation(summary = "보고서 PDF 단일 업로드", description = "관리자용 보고서 PDF 업로드 API")
//	public ResponseEntity<ApiResponse<ReportPdfPublishResult>> uploadPdf(
//		@RequestParam("stockCode") String stockCode,
//		@RequestParam("quarterKey") int quarterKey,
//		@RequestParam("file") MultipartFile file
//	) {
//		if (file == null || file.isEmpty()) {
//			throw new CommonException(CommonErrorCode.COMMON_400);
//		}
//
//		ReportPdfPublishResult result = companyReportPdfPublishService.publishPdfOnly(stockCode, quarterKey, file);
//		return ResponseEntity.ok(ApiResponse.ok(result));
//	}
//}
