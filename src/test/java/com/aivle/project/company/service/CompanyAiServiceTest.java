package com.aivle.project.company.service;

import com.aivle.project.company.client.AiServerClient;
import com.aivle.project.company.dto.AiAnalysisResponse;
import com.aivle.project.company.entity.CompaniesEntity;
import com.aivle.project.company.repository.CompaniesRepository;
import com.aivle.project.file.entity.FileUsageType;
import com.aivle.project.file.entity.FilesEntity;
import com.aivle.project.file.repository.FilesRepository;
import com.aivle.project.file.storage.FileStorageService;
import com.aivle.project.file.storage.StoredFile;
import com.aivle.project.metric.entity.MetricsEntity;
import com.aivle.project.metric.repository.MetricsRepository;
import com.aivle.project.quarter.entity.QuartersEntity;
import com.aivle.project.quarter.repository.QuartersRepository;
import com.aivle.project.report.entity.CompanyReportVersionsEntity;
import com.aivle.project.report.entity.CompanyReportsEntity;
import com.aivle.project.report.repository.CompanyReportMetricValuesRepository;
import com.aivle.project.report.repository.CompanyReportVersionsRepository;
import com.aivle.project.report.repository.CompanyReportsRepository;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CompanyAiServiceTest {

    @InjectMocks
    private CompanyAiService companyAiService;

    @Mock
    private AiServerClient aiServerClient;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private FilesRepository filesRepository;

    @Mock
    private CompaniesRepository companiesRepository;

    @Mock
    private QuartersRepository quartersRepository;

    @Mock
    private CompanyReportsRepository companyReportsRepository;

    @Mock
    private CompanyReportVersionsRepository companyReportVersionsRepository;

    @Mock
    private MetricsRepository metricsRepository;

    @Mock
    private CompanyReportMetricValuesRepository companyReportMetricValuesRepository;

    @Test
    @DisplayName("AI 예측 분석 결과를 조회하고 저장한다 (Cache Miss)")
    void getCompanyAnalysis_Success() {
        // given
        Long companyId = 1L;
        String companyCode = "005930";
        AiAnalysisResponse response = new AiAnalysisResponse(
            companyCode,
            "삼성전자",
            "20253",
            Map.of("ROA", 5.5)
        );

        CompaniesEntity company = CompaniesEntity.create("00000001", "삼성전자", null, companyCode, LocalDate.now());
        QuartersEntity targetQuarter = QuartersEntity.create(2025, 4, 20254, LocalDate.now(), LocalDate.now());

        given(companiesRepository.findById(companyId)).willReturn(Optional.of(company));
        given(companyReportMetricValuesRepository.findMaxActualQuarterKeyByCompanyId(companyId)).willReturn(Optional.of(20253));
        given(companyReportMetricValuesRepository.findLatestMetricsByCompanyIdAndQuarterKeyAndType(
            eq(companyId), eq(20254), eq(com.aivle.project.metric.entity.MetricValueType.PREDICTED)))
            .willReturn(Collections.emptyList());

        given(aiServerClient.getPrediction(companyCode)).willReturn(response);

        // when
        AiAnalysisResponse result = companyAiService.getCompanyAnalysis(companyId, null, null);

        // then
        assertThat(result).isEqualTo(response);
        verify(aiServerClient).getPrediction(companyCode);
        // saveAiPredictions가 실행되지만 try-catch로 인해 예외가 발생해도 테스트는 통과됨
    }

    @Test
    @DisplayName("DB에 이미 예측치가 있으면 AI 서버를 호출하지 않고 DB 값을 반환한다 (Cache Hit)")
    void getCompanyAnalysis_CacheHit() {
        // given
        Long companyId = 1L;
        String companyCode = "005930";
        CompaniesEntity company = CompaniesEntity.create("00000001", "삼성전자", null, companyCode, LocalDate.now());

        // Mock DB projection
        com.aivle.project.report.dto.ReportPredictMetricRowProjection mockProj = org.mockito.Mockito.mock(com.aivle.project.report.dto.ReportPredictMetricRowProjection.class);
        given(mockProj.getMetricCode()).willReturn("ROA");
        given(mockProj.getMetricValue()).willReturn(java.math.BigDecimal.valueOf(5.5));

        given(companiesRepository.findById(companyId)).willReturn(Optional.of(company));
        given(companyReportMetricValuesRepository.findMaxActualQuarterKeyByCompanyId(companyId)).willReturn(Optional.of(20253));
        given(companyReportMetricValuesRepository.findLatestMetricsByCompanyIdAndQuarterKeyAndType(
            eq(companyId), eq(20254), eq(com.aivle.project.metric.entity.MetricValueType.PREDICTED)))
            .willReturn(List.of(mockProj));

        // when
        AiAnalysisResponse result = companyAiService.getCompanyAnalysis(companyId, null, null);

        // then
        assertThat(result.predictions()).containsEntry("ROA", 5.5);
        assertThat(result.basePeriod()).isEqualTo("20253");
        verify(aiServerClient, org.mockito.Mockito.never()).getPrediction(any());
    }

    @Test
    @DisplayName("연도와 분기가 제공되지 않으면 현재 날짜 기준 분기를 조회/생성하여 연결한다")
    void generateAndSaveReport_WithAutomaticQuarter() {
        // given
        Long companyId = 1L;
        String companyCode = "005930";
        LocalDate today = LocalDate.now();
        int expectedYear = today.getYear();
        int expectedQuarter = (today.getMonthValue() - 1) / 3 + 1;

        byte[] pdfContent = "dummy-pdf".getBytes();
        StoredFile storedFile = new StoredFile(
            "http://localhost/files/report.pdf",
            "report_005930.pdf",
            100L,
            "application/pdf",
            "reports/report_005930.pdf"
        );
        FilesEntity savedEntity = FilesEntity.create(
            FileUsageType.REPORT_PDF,
            storedFile.storageUrl(),
            storedFile.storageKey(),
            storedFile.originalFilename(),
            storedFile.fileSize(),
            storedFile.contentType()
        );

        CompaniesEntity company = CompaniesEntity.create("00000001", "삼성전자", null, companyCode, LocalDate.now());
        // 처음에는 분기가 없다고 가정
        given(aiServerClient.getAnalysisReportPdf(companyCode)).willReturn(pdfContent);
        given(fileStorageService.store(any(MultipartFile.class), any())).willReturn(storedFile);
        given(filesRepository.save(any(FilesEntity.class))).willReturn(savedEntity);
        given(companiesRepository.findById(companyId)).willReturn(Optional.of(company));

        // 분기 조회 시 empty 반환 -> 생성 로직 유도
        given(quartersRepository.findByYearAndQuarter((short) expectedYear, (byte) expectedQuarter)).willReturn(Optional.empty());
        // 생성된 분기 모킹
        QuartersEntity newQuarter = QuartersEntity.create(expectedYear, expectedQuarter, expectedYear * 10 + expectedQuarter, today, today);
        given(quartersRepository.save(any(QuartersEntity.class))).willReturn(newQuarter);

        given(companyReportsRepository.findByCompanyIdAndQuarterId(any(), any())).willReturn(Optional.empty());
        given(companyReportsRepository.save(any(CompanyReportsEntity.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(companyReportVersionsRepository.findTopByCompanyReportOrderByVersionNoDesc(any())).willReturn(Optional.empty());

        // when
        FilesEntity result = companyAiService.generateAndSaveReport(companyId, null, null);

        // then
        assertThat(result).isNotNull();
        verify(quartersRepository).save(any(QuartersEntity.class)); // 분기 생성 확인
        verify(companyReportsRepository).save(any(CompanyReportsEntity.class)); // 보고서 생성 확인
        verify(companyReportVersionsRepository).save(any(CompanyReportVersionsEntity.class)); // 버전 생성 확인
    }

    @Test
    @DisplayName("AI 리포트를 생성하고 저장한다 (연도/분기 미포함 -> 현재 분기 자동 연결)")
    void generateAndSaveReport_Success() {
        // given
        Long companyId = 1L;
        String companyCode = "005930";
        byte[] pdfContent = "dummy-pdf".getBytes();
        StoredFile storedFile = new StoredFile(
            "http://localhost/files/report.pdf",
            "report_005930.pdf",
            100L,
            "application/pdf",
            "reports/report_005930.pdf"
        );
        FilesEntity savedEntity = FilesEntity.create(
            FileUsageType.REPORT_PDF,
            storedFile.storageUrl(),
            storedFile.storageKey(),
            storedFile.originalFilename(),
            storedFile.fileSize(),
            storedFile.contentType()
        );

        CompaniesEntity company = CompaniesEntity.create("00000001", "삼성전자", null, companyCode, LocalDate.now());
        QuartersEntity quarterEntity = QuartersEntity.create(2026, 1, 20261, LocalDate.now(), LocalDate.now());

        given(aiServerClient.getAnalysisReportPdf(companyCode)).willReturn(pdfContent);
        given(fileStorageService.store(any(MultipartFile.class), eq("reports/005930/2026/1"))).willReturn(storedFile);
        given(filesRepository.save(any(FilesEntity.class))).willReturn(savedEntity);
        given(companiesRepository.findById(companyId)).willReturn(Optional.of(company));
        given(quartersRepository.findByYearAndQuarter(any(Short.class), any(Byte.class))).willReturn(Optional.of(quarterEntity));
        given(companyReportsRepository.findByCompanyIdAndQuarterId(any(), any())).willReturn(Optional.empty());
        given(companyReportsRepository.save(any(CompanyReportsEntity.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        FilesEntity result = companyAiService.generateAndSaveReport(companyId, null, null);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getStorageUrl()).isEqualTo(storedFile.storageUrl());

        verify(aiServerClient).getAnalysisReportPdf(companyCode);
        verify(filesRepository, atLeastOnce()).save(any(FilesEntity.class));
    }

    @Test
    @DisplayName("연도와 분기가 제공되면 AI 리포트를 생성하고 보고서 버전으로 연결한다")
    void generateAndSaveReport_WithVersionLink() {
        // given
        Long companyId = 1L;
        String companyCode = "005930";
        Integer year = 2026;
        Integer quarter = 1;

        byte[] pdfContent = "dummy-pdf".getBytes();
        StoredFile storedFile = new StoredFile(
            "http://localhost/files/report.pdf",
            "report_005930.pdf",
            100L,
            "application/pdf",
            "reports/report_005930.pdf"
        );
        FilesEntity savedEntity = FilesEntity.create(
            FileUsageType.REPORT_PDF,
            storedFile.storageUrl(),
            storedFile.storageKey(),
            storedFile.originalFilename(),
            storedFile.fileSize(),
            storedFile.contentType()
        );

        CompaniesEntity company = CompaniesEntity.create("00000001", "삼성전자", null, companyCode, LocalDate.now());
        QuartersEntity quarterEntity = QuartersEntity.create(year, quarter, 20261, LocalDate.now(), LocalDate.now());
        CompanyReportsEntity report = CompanyReportsEntity.create(company, quarterEntity, null);

        given(aiServerClient.getAnalysisReportPdf(companyCode)).willReturn(pdfContent);
        given(fileStorageService.store(any(MultipartFile.class), eq("reports/005930/2026/1"))).willReturn(storedFile);
        given(filesRepository.save(any(FilesEntity.class))).willReturn(savedEntity);
        given(companiesRepository.findById(companyId)).willReturn(Optional.of(company));
        given(quartersRepository.findByYearAndQuarter(year.shortValue(), quarter.byteValue())).willReturn(Optional.of(quarterEntity));
        given(companyReportsRepository.findByCompanyIdAndQuarterId(any(), any())).willReturn(Optional.of(report));
        given(companyReportVersionsRepository.findTopByCompanyReportOrderByVersionNoDesc(any())).willReturn(Optional.empty());

        // when
        FilesEntity result = companyAiService.generateAndSaveReport(companyId, year, quarter);

        // then
        assertThat(result).isNotNull();
        verify(companyReportVersionsRepository).save(any(CompanyReportVersionsEntity.class));
    }
}