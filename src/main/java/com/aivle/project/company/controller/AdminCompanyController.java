package com.aivle.project.company.controller;

import com.aivle.project.common.dto.ApiResponse;
import com.aivle.project.company.service.CompanyIndustryImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Tag(name = "Admin Company Management", description = "관리자용 기업 관리 API")
@RestController
@RequestMapping("/api/admin/companies")
@RequiredArgsConstructor
public class AdminCompanyController {

    private final CompanyIndustryImportService industryImportService;

    @Operation(summary = "기업 업종 정보 엑셀 업로드", description = "엑셀 파일을 통해 기업의 industry_code_id를 일괄 업데이트합니다.")
    @PostMapping(value = "/industry/excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<String> uploadIndustryExcel(@RequestParam("file") MultipartFile file) throws IOException {
        industryImportService.importCompanyIndustries(file);
        return ApiResponse.ok("기업 업종 정보 업데이트가 완료되었습니다.");
    }
}
