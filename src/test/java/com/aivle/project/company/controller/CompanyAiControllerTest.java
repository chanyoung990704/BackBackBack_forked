package com.aivle.project.company.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aivle.project.company.dto.AiAnalysisResponse;
import com.aivle.project.company.job.AiJobDispatchService;
import com.aivle.project.company.service.CompanyAiService;
import com.aivle.project.file.entity.FileUsageType;
import com.aivle.project.file.entity.FilesEntity;
import com.aivle.project.common.config.TestSecurityConfig;
import com.aivle.project.file.storage.FileStreamService;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class CompanyAiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CompanyAiService companyAiService;

    @MockBean
    private FileStreamService fileStreamService;

    @MockBean
    private AiJobDispatchService aiJobDispatchService;

    @Test
    @DisplayName("ROLE_USER로 기업 AI 분석을 조회한다")
    void getCompanyAnalysis() throws Exception {
        // given
        AiAnalysisResponse response = new AiAnalysisResponse(
            "005930",
            "삼성전자",
            "2025Q4",
            Map.of("ROA", 1.23)
        );
        given(companyAiService.getCompanyAnalysis(5930L, null, null)).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/companies/5930/ai-analysis")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.company_code").value("005930"))
            .andExpect(jsonPath("$.data.company_name").value("삼성전자"))
            .andExpect(jsonPath("$.data.base_period").value("2025Q4"))
            .andExpect(jsonPath("$.data.predictions.ROA").value(1.23));
    }

    @Test
    @DisplayName("신규 경로(/analysis)로 기업 AI 분석을 조회한다")
    void getCompanyAnalysis_withNewPath() throws Exception {
        // given
        AiAnalysisResponse response = new AiAnalysisResponse(
            "005930",
            "삼성전자",
            "2025Q4",
            Map.of("ROA", 1.23)
        );
        given(companyAiService.getCompanyAnalysis(5930L, null, null)).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/companies/5930/analysis")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.company_code").value("005930"));
    }

    @Test
    @DisplayName("기업 AI 분석 조회는 인증이 없으면 401을 반환한다")
    void getCompanyAnalysis_unauthorized() throws Exception {
        // when & then
        mockMvc.perform(get("/api/companies/005930/ai-analysis"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("기업 AI 분석 조회는 ROLE_ADMIN도 접근 가능하다 (권한 계층)")
    void getCompanyAnalysis_allowedForAdmin() throws Exception {
        // given
        AiAnalysisResponse response = new AiAnalysisResponse(
            "005930",
            "삼성전자",
            "2025Q4",
            Map.of("ROA", 1.23)
        );
        given(companyAiService.getCompanyAnalysis(5930L, null, null)).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/companies/5930/ai-analysis")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("ROLE_ADMIN으로 기업 AI 리포트 PDF를 생성/저장한다 (연도/분기 미포함)")
    void generateCompanyAiReport() throws Exception {
        // given
        FilesEntity file = FilesEntity.create(
            FileUsageType.REPORT_PDF,
            "http://localhost/files/report_005930.pdf",
            "reports/report_005930.pdf",
            "report_005930.pdf",
            1024L,
            "application/pdf"
        );
        given(companyAiService.generateAndSaveReport(5930L, null, null)).willReturn(file);

        // when & then
        mockMvc.perform(post("/api/companies/5930/ai-report")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.storageUrl").value("http://localhost/files/report_005930.pdf"));
    }

    @Test
    @DisplayName("신규 경로(/ai-reports)로 기업 AI 리포트 PDF를 생성/저장한다")
    void generateCompanyAiReport_withNewPath() throws Exception {
        // given
        FilesEntity file = FilesEntity.create(
            FileUsageType.REPORT_PDF,
            "http://localhost/files/report_005930.pdf",
            "reports/report_005930.pdf",
            "report_005930.pdf",
            1024L,
            "application/pdf"
        );
        given(companyAiService.generateAndSaveReport(5930L, null, null)).willReturn(file);

        // when & then
        mockMvc.perform(post("/api/companies/5930/ai-reports")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("ROLE_ADMIN으로 기업 AI 리포트 PDF를 생성/저장한다 (연도/분기 포함)")
    void generateCompanyAiReport_withYearAndQuarter() throws Exception {
        // given
        FilesEntity file = FilesEntity.create(
            FileUsageType.REPORT_PDF,
            "http://localhost/files/report_005930.pdf",
            "reports/report_005930.pdf",
            "report_005930.pdf",
            1024L,
            "application/pdf"
        );
        given(companyAiService.generateAndSaveReport(5930L, 2026, 1)).willReturn(file);

        // when & then
        mockMvc.perform(post("/api/companies/5930/ai-report")
                .param("year", "2026")
                .param("quarter", "1")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("기업 AI 리포트 생성은 ROLE_USER도 접근 가능하다")
    void generateCompanyAiReport_allowedForUser() throws Exception {
        // given
        FilesEntity file = FilesEntity.create(
            FileUsageType.REPORT_PDF,
            "http://localhost/files/report_005930.pdf",
            "reports/report_005930.pdf",
            "report_005930.pdf",
            1024L,
            "application/pdf"
        );
        given(companyAiService.generateAndSaveReport(5930L, null, null)).willReturn(file);

        // when & then
        mockMvc.perform(post("/api/companies/5930/ai-report")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.storageUrl").value("http://localhost/files/report_005930.pdf"));
    }

    @Test
    @DisplayName("기업 AI 리포트 PDF를 스트리밍으로 다운로드한다")
    void downloadAiReport_streamsFile() throws Exception {
        // given
        FilesEntity file = FilesEntity.create(
            FileUsageType.REPORT_PDF,
            "https://bucket.s3.ap-northeast-2.amazonaws.com/reports/a.pdf",
            "reports/a.pdf",
            "report_005930.pdf",
            12L,
            "application/pdf"
        );
        given(companyAiService.getReportFileById(5930L, 2026, 1)).willReturn(file);
        given(fileStreamService.openStream(file))
            .willReturn(new ByteArrayInputStream("report".getBytes(StandardCharsets.UTF_8)));

        // when & then
        mockMvc.perform(get("/api/companies/5930/ai-report/download")
                .param("year", "2026")
                .param("quarter", "1")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", "application/pdf"))
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"report_005930.pdf\""))
            .andExpect(header().string("Cache-Control", "private, no-store"));
    }

    @Test
    @DisplayName("신규 경로(/ai-reports/file)로 기업 AI 리포트를 스트리밍 다운로드한다")
    void downloadAiReport_withNewPath_streamsFile() throws Exception {
        // given
        FilesEntity file = FilesEntity.create(
            FileUsageType.REPORT_PDF,
            "https://bucket.s3.ap-northeast-2.amazonaws.com/reports/a.pdf",
            "reports/a.pdf",
            "report_005930.pdf",
            12L,
            "application/pdf"
        );
        given(companyAiService.getReportFileById(5930L, 2026, 1)).willReturn(file);
        given(fileStreamService.openStream(file))
            .willReturn(new ByteArrayInputStream("report".getBytes(StandardCharsets.UTF_8)));

        // when & then
        mockMvc.perform(get("/api/companies/5930/ai-reports/file")
                .param("year", "2026")
                .param("quarter", "1")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", "application/pdf"))
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"report_005930.pdf\""))
            .andExpect(header().string("Cache-Control", "private, no-store"));
    }

    @org.junit.jupiter.api.Disabled
    @Test
    @DisplayName("기업 AI 리포트 PDF를 ID 기준으로 스트리밍 다운로드한다")
    void downloadAiReportById_streamsFile() throws Exception {
        // given
        FilesEntity file = FilesEntity.create(
            FileUsageType.REPORT_PDF,
            "https://bucket.s3.ap-northeast-2.amazonaws.com/reports/b.pdf",
            "reports/b.pdf",
            "report_005930.pdf",
            12L,
            "application/pdf"
        );
        // given(companyAiService.getReportFileById(1L, 2026, 1)).willReturn(file);
        // given(fileStreamService.openStream(file))
        //    .willReturn(new ByteArrayInputStream("report".getBytes(StandardCharsets.UTF_8)));

        // when & then
        mockMvc.perform(get("/api/companies/id/1/ai-report/download")
                .param("year", "2026")
                .param("quarter", "1")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", "application/pdf"))
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"report_005930.pdf\""))
            .andExpect(header().string("Cache-Control", "private, no-store"));
    }
}
