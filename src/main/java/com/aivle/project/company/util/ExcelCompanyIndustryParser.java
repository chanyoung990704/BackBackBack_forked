package com.aivle.project.company.util;

import com.aivle.project.company.dto.CompanyIndustryImportDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Slf4j
@Component
public class ExcelCompanyIndustryParser {

    /**
     * 기업 업종 정보 엑셀 파싱.
     * 형식: 기업코드 | 기업명 | 업종코드 | 업종명
     */
    public List<CompanyIndustryImportDto> parse(InputStream is) throws IOException {
        List<CompanyIndustryImportDto> dtos = new ArrayList<>();
        try (Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();

            // 첫 번째 행(헤더) 건너뛰기
            if (rows.hasNext()) {
                rows.next();
            }

            while (rows.hasNext()) {
                Row currentRow = rows.next();
                try {
                    String stockCode = getStockCodeValue(currentRow.getCell(0));
                    String corpName = getCellValue(currentRow.getCell(1));
                    String industryCode = getIndustryCodeValue(currentRow.getCell(2));
                    String industryName = getCellValue(currentRow.getCell(3));

                    if (stockCode != null && !stockCode.isBlank()) {
                        dtos.add(new CompanyIndustryImportDto(stockCode, corpName, industryCode, industryName));
                    }
                } catch (Exception e) {
                    log.error("Error parsing row {}: {}", currentRow.getRowNum(), e.getMessage());
                }
            }
        }
        return dtos;
    }

    private String getStockCodeValue(Cell cell) {
        String val = getCellValue(cell);
        if (val == null || val.isBlank()) return null;
        
        // 6자리 미만이면 앞에 0 채우기 (문자가 포함되어 있어도 적용)
        if (val.length() < 6) {
            return String.format("%6s", val).replace(' ', '0');
        }
        return val;
    }

    private String getIndustryCodeValue(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) {
            return String.valueOf((int) cell.getNumericCellValue());
        }
        return cell.getStringCellValue().trim();
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            default -> null;
        };
    }
}
