package com.aivle.project.company;

import com.aivle.project.company.entity.CompaniesEntity;
import com.aivle.project.company.repository.CompaniesRepository;
import com.aivle.project.company.service.CompanyIndustryImportService;
import com.aivle.project.industry.entity.IndustryEntity;
import com.aivle.project.industry.entity.IndustryRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CompanyIndustryImportIntegrationTest {

    @Autowired
    private CompanyIndustryImportService industryImportService;

    @Autowired
    private CompaniesRepository companiesRepository;

    @Autowired
    private IndustryRepository industryRepository;

    @Test
    @DisplayName("기업 업종 엑셀 업로드 시 기업의 industry_code가 정상적으로 업데이트된다")
    void importCompanyIndustries_Success() throws IOException {
        // given: 테스트 데이터 준비 (DB)
        IndustryEntity industry = industryRepository.save(IndustryEntity.create("10", "음식료품 제조업"));
        
        CompaniesEntity company = companiesRepository.save(CompaniesEntity.create(
            "00000001", "삼성전자", "Samsung Electronics", "005930", LocalDate.now()
        ));
        
        // 엑셀 파일 생성
        byte[] excelBytes = createExcelFile("5930", "삼성전자", "10", "음식료품 제조업");
        MockMultipartFile file = new MockMultipartFile("file", "test.xlsx", 
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", excelBytes);

        // when
        industryImportService.importCompanyIndustries(file);

        // then
        CompaniesEntity updatedCompany = companiesRepository.findByStockCode("005930").get();
        assertThat(updatedCompany.getIndustryCode()).isNotNull();
        assertThat(updatedCompany.getIndustryCode().getIndustryCode()).isEqualTo("10");
        assertThat(updatedCompany.getIndustryCode().getIndustryName()).isEqualTo("음식료품 제조업");
    }

    @Test
    @DisplayName("기업 코드에 문자가 포함되어 있어도 6자리 패딩이 적용되어 정상적으로 업데이트된다")
    void importCompanyIndustries_WithAlphaNumericStockCode() throws IOException {
        // given: 문자가 포함된 stockCode를 가진 기업 준비
        industryRepository.save(IndustryEntity.create("20", "IT 서비스"));
        companiesRepository.save(CompaniesEntity.create(
            "00000002", "알파기업", "Alpha Co", "00A123", LocalDate.now()
        ));

        // 엑셀에는 'A123'으로 입력 (파서가 '00A123'으로 변환해야 함)
        byte[] excelBytes = createExcelFile("A123", "알파기업", "20", "IT 서비스");
        MockMultipartFile file = new MockMultipartFile("file", "test_alpha.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", excelBytes);

        // when
        industryImportService.importCompanyIndustries(file);

        // then
        CompaniesEntity updatedCompany = companiesRepository.findByStockCode("00A123").get();
        assertThat(updatedCompany.getIndustryCode()).isNotNull();
        assertThat(updatedCompany.getIndustryCode().getIndustryCode()).isEqualTo("20");
    }

    private byte[] createExcelFile(String stockCode, String corpName, String indCode, String indName) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet();
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("기업코드");
            header.createCell(1).setCellValue("기업명");
            header.createCell(2).setCellValue("업종코드");
            header.createCell(3).setCellValue("업종명");

            Row dataRow = sheet.createRow(1);
            dataRow.createCell(0).setCellValue(stockCode);
            dataRow.createCell(1).setCellValue(corpName);
            dataRow.createCell(2).setCellValue(indCode);
            dataRow.createCell(3).setCellValue(indName);

            workbook.write(bos);
            return bos.toByteArray();
        }
    }
}
