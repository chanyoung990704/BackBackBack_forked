package com.aivle.project.report;

import com.aivle.project.company.entity.CompaniesEntity;
import com.aivle.project.company.repository.CompaniesRepository;
import com.aivle.project.metric.entity.MetricValueType;
import com.aivle.project.metric.entity.MetricsEntity;
import com.aivle.project.metric.repository.MetricsRepository;
import com.aivle.project.quarter.entity.QuartersEntity;
import com.aivle.project.quarter.repository.QuartersRepository;
import com.aivle.project.report.entity.CompanyReportMetricValuesEntity;
import com.aivle.project.report.entity.CompanyReportVersionsEntity;
import com.aivle.project.report.entity.CompanyReportsEntity;
import com.aivle.project.report.repository.CompanyReportMetricValuesRepository;
import com.aivle.project.report.repository.CompanyReportVersionsRepository;
import com.aivle.project.report.repository.CompanyReportsRepository;
import com.aivle.project.report.util.ExcelIndustryMetricParser;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class IndustryMetricImportIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CompaniesRepository companiesRepository;

    @Autowired
    private MetricsRepository metricsRepository;

    @Autowired
    private QuartersRepository quartersRepository;

    @Autowired
    private CompanyReportMetricValuesRepository metricValuesRepository;

    @Test
    @DisplayName("업종 상대 지표 엑셀 업로드 통합 테스트")
    @WithMockUser(roles = "ADMIN")
    void importIndustryMetricsExcel() throws Exception {
        // Given: 테스트 데이터 준비
        CompaniesEntity samsung = companiesRepository.save(CompaniesEntity.create(
            "00593000", "삼성전자", "Samsung Electronics", "005930", java.time.LocalDate.now()
        ));
        
        // 지표 생성 (V20 마이그레이션이 이미 실행되었을 것이나, 테스트 환경을 위해 명시적으로 확인 또는 생성)
        MetricsEntity roaMetric = metricsRepository.findByMetricCode("ROA_IndRel")
                .orElseGet(() -> metricsRepository.save(createMetric("ROA_IndRel", "ROA 업종상대")));
        
        // 엑셀 파일 생성
        byte[] excelContent = createExcelContent("005930", 1.5, 1.2, 1.0, 0.8);
        MockMultipartFile file = new MockMultipartFile("file", "test.xlsx", 
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", excelContent);

        // When: API 호출 (2024년 1분기 기준)
        mockMvc.perform(multipart("/api/admin/reports/metrics/industry-excel")
                .file(file)
                .param("year", "2024")
                .param("quarter", "1"))
                .andExpect(status().isOk());

        // Then: 데이터 저장 확인
        // 2024 1Q (현재), 2023 4Q (-1), 2023 3Q (-2), 2023 2Q (-3)
        List<QuartersEntity> targetQuarters = quartersRepository.findAll();
        assertThat(targetQuarters).hasSizeGreaterThanOrEqualTo(4);

        List<CompanyReportMetricValuesEntity> values = metricValuesRepository.findAll();
        // ROA 지표 4개 분기 + 다른 4개 지표들(빈값이면 안들어감)
        // 여기서는 ROA만 값을 채웠으므로 4개가 들어와야 함
        assertThat(values.stream()
                .filter(v -> v.getMetric().getMetricCode().equals("ROA_IndRel"))
                .count()).isEqualTo(4);

        CompanyReportMetricValuesEntity currentVal = values.stream()
                .filter(v -> v.getMetric().getMetricCode().equals("ROA_IndRel") && v.getQuarter().getQuarterKey() == 20241)
                .findFirst().orElseThrow();
        assertThat(currentVal.getMetricValue().doubleValue()).isEqualTo(1.5);

        CompanyReportMetricValuesEntity prev1Val = values.stream()
                .filter(v -> v.getMetric().getMetricCode().equals("ROA_IndRel") && v.getQuarter().getQuarterKey() == 20234)
                .findFirst().orElseThrow();
        assertThat(prev1Val.getMetricValue().doubleValue()).isEqualTo(1.2);
    }

    @Test
    @DisplayName("업종 상대 지표 엑셀 업로드 통합 테스트 (문자 포함 기업코드 및 5종 지표)")
    @WithMockUser(roles = "ADMIN")
    void importIndustryMetricsExcel_Complete() throws Exception {
        // Given: 테스트 데이터 준비 (문자 포함 기업코드)
        CompaniesEntity alphaComp = companiesRepository.save(CompaniesEntity.create(
            "00000002", "알파기업", "Alpha Co", "00A123", java.time.LocalDate.now()
        ));
        
        // 지표 생성 (V20 마이그레이션이 이미 실행되었을 것이나, 테스트 환경을 위해 명시적으로 생성)
        metricsRepository.save(createMetric("ROA_IndRel", "ROA 업종상대"));
        metricsRepository.save(createMetric("DbRatio_IndRel", "부채비율 업종상대"));
        metricsRepository.save(createMetric("STDebtRatio_IndRel", "단기차입금비율 업종상대"));
        metricsRepository.save(createMetric("CurRatio_IndRel", "유동비율 업종상대"));
        metricsRepository.save(createMetric("CFO_AsRatio_IndRel", "CFO 자산비율 업종상대"));
        
        // 엑셀 파일 생성 (A123 기업에 대해 5종 지표 값 채우기)
        // ROA(4~7), Db(8~11), ST(12~15), Cur(16~19), CFO(20~23)
        byte[] excelContent = createFullExcelContent("A123");
        MockMultipartFile file = new MockMultipartFile("file", "test_complete.xlsx", 
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", excelContent);

        // When: API 호출 (2024년 1분기 기준)
        mockMvc.perform(multipart("/api/admin/reports/metrics/industry-excel")
                .file(file)
                .param("year", "2024")
                .param("quarter", "1"))
                .andExpect(status().isOk());

        // Then: 데이터 저장 확인 (00A123 기업)
        List<CompanyReportMetricValuesEntity> values = metricValuesRepository.findAll();
        
        // 특정 지표(ROA_IndRel)의 20241 분기 값이 1.5인지 확인
        assertThat(values.stream()
                .filter(v -> v.getMetric().getMetricCode().equals("ROA_IndRel") 
                          && v.getQuarter().getQuarterKey() == 20241
                          && v.getReportVersion().getCompanyReport().getCompany().getStockCode().equals("00A123"))
                .findFirst().get().getMetricValue().doubleValue()).isEqualTo(1.5);

        // CFO_AsRatio_IndRel 지표도 들어왔는지 확인
        assertThat(values.stream()
                .filter(v -> v.getMetric().getMetricCode().equals("CFO_AsRatio_IndRel")
                          && v.getReportVersion().getCompanyReport().getCompany().getStockCode().equals("00A123"))
                .count()).isEqualTo(4);
    }

    private byte[] createFullExcelContent(String stockCode) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet();
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("기업코드");
            header.createCell(4).setCellValue("ROA_현재");
            header.createCell(8).setCellValue("부채_현재");
            header.createCell(12).setCellValue("단기차입_현재");
            header.createCell(16).setCellValue("유동_현재");
            header.createCell(20).setCellValue("CFO_현재");

            Row dataRow = sheet.createRow(1);
            dataRow.createCell(0).setCellValue(stockCode);
            // 각 지표의 '현재' 컬럼에 값 채우기 (나머지 분기도 자동으로 채워짐 - Parser 로직상 0이면 skip되지 않음)
            for (int i = 4; i <= 20; i += 4) {
                dataRow.createCell(i).setCellValue(1.5); // 현재
                dataRow.createCell(i+1).setCellValue(1.2); // -1
                dataRow.createCell(i+2).setCellValue(1.0); // -2
                dataRow.createCell(i+3).setCellValue(0.8); // -3
            }

            workbook.write(bos);
            return bos.toByteArray();
        }
    }

    @Test
    @DisplayName("업종 상대 지표 엑셀 업로드 시 0은 0으로, 빈 값은 null로 저장된다")
    @WithMockUser(roles = "ADMIN")
    void importIndustryMetricsExcel_ZeroAndNull() throws Exception {
        // Given
        companiesRepository.save(CompaniesEntity.create(
            "00000003", "베타기업", "Beta Co", "00B456", java.time.LocalDate.now()
        ));
        metricsRepository.save(createMetric("ROA_IndRel", "ROA 업종상대"));
        metricsRepository.save(createMetric("DbRatio_IndRel", "부채비율 업종상대"));

        // 엑셀 생성: ROA는 0.0, 부채비율은 빈 값(-)
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet();
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("기업코드");
            header.createCell(4).setCellValue("ROA_현재");
            header.createCell(8).setCellValue("부채_현재");

            Row dataRow = sheet.createRow(1);
            dataRow.createCell(0).setCellValue("B456");
            dataRow.createCell(4).setCellValue(0.0); // ROA: 0.0
            dataRow.createCell(8).setCellValue("-");   // 부채: 빈 값 처리 대상

            workbook.write(bos);
            
            MockMultipartFile file = new MockMultipartFile("file", "test_zero_null.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bos.toByteArray());

            // When
            mockMvc.perform(multipart("/api/admin/reports/metrics/industry-excel")
                    .file(file)
                    .param("year", "2024")
                    .param("quarter", "1"))
                    .andExpect(status().isOk());
        }

        // Then
        List<CompanyReportMetricValuesEntity> values = metricValuesRepository.findAll();
        
        // ROA_현재가 0.0으로 저장되었는지 확인
        assertThat(values.stream()
                .filter(v -> v.getMetric().getMetricCode().equals("ROA_IndRel") 
                          && v.getQuarter().getQuarterKey() == 20241
                          && v.getReportVersion().getCompanyReport().getCompany().getStockCode().equals("00B456"))
                .findFirst().get().getMetricValue().doubleValue()).isEqualTo(0.0);

        // DbRatio_IndRel 현재가 null로 저장되었는지 확인
        assertThat(values.stream()
                .filter(v -> v.getMetric().getMetricCode().equals("DbRatio_IndRel") 
                          && v.getQuarter().getQuarterKey() == 20241
                          && v.getReportVersion().getCompanyReport().getCompany().getStockCode().equals("00B456"))
                .findFirst().get().getMetricValue()).isNull();
    }

    private MetricsEntity createMetric(String code, String name) {
        return MetricsEntity.create(code, name, code, true);
    }

    private byte[] createExcelContent(String stockCode, double cur, double m1, double m2, double m3) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet();
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("기업코드");
            header.createCell(4).setCellValue("ROA_현재");
            header.createCell(5).setCellValue("ROA_-1");
            header.createCell(6).setCellValue("ROA_-2");
            header.createCell(7).setCellValue("ROA_-3");

            Row dataRow = sheet.createRow(1);
            dataRow.createCell(0).setCellValue(stockCode);
            dataRow.createCell(4).setCellValue(cur);
            dataRow.createCell(5).setCellValue(m1);
            dataRow.createCell(6).setCellValue(m2);
            dataRow.createCell(7).setCellValue(m3);

            workbook.write(bos);
            return bos.toByteArray();
        }
    }
}
