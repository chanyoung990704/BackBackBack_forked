package com.aivle.project.report.util;

import com.aivle.project.report.dto.CompanyMetricValueCommand;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class ExcelIndustryMetricParser {

    private static final int ROA_START_IDX = 4;
    private static final int DEBT_START_IDX = 8;
    private static final int SHORT_DEBT_START_IDX = 12;
    private static final int CUR_RATIO_START_IDX = 16;
    private static final int CFO_START_IDX = 20;

    public List<CompanyMetricValueCommand> parse(org.springframework.web.multipart.MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null && (originalFilename.endsWith(".csv") || originalFilename.endsWith(".CSV"))) {
            return parseCsv(file.getInputStream());
        }
        return parseExcel(file.getInputStream());
    }

    private List<CompanyMetricValueCommand> parseExcel(InputStream is) throws IOException {
        List<CompanyMetricValueCommand> commands = new ArrayList<>();
        try (Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String stockCode = getStockCodeValue(row.getCell(0));
                if (stockCode == null || stockCode.isBlank()) continue;

                extractMetrics(commands, row, i, stockCode, "ROA_IndRel", ROA_START_IDX);
                extractMetrics(commands, row, i, stockCode, "DbRatio_IndRel", DEBT_START_IDX);
                extractMetrics(commands, row, i, stockCode, "STDebtRatio_IndRel", SHORT_DEBT_START_IDX);
                extractMetrics(commands, row, i, stockCode, "CurRatio_IndRel", CUR_RATIO_START_IDX);
                extractMetrics(commands, row, i, stockCode, "CFO_AsRatio_IndRel", CFO_START_IDX);
            }
        }
        return commands;
    }

    private List<CompanyMetricValueCommand> parseCsv(InputStream is) throws IOException {
        List<CompanyMetricValueCommand> commands = new ArrayList<>();
        // 한국어 CSV는 보통 MS949(EUC-KR) 인코딩을 사용함. 필요시 UTF-8로 변경.
        try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(is, "MS949"))) {
            String line;
            int rowIndex = 0;
            while ((line = br.readLine()) != null) {
                if (rowIndex == 0) { // 헤더 스킵
                    rowIndex++;
                    continue;
                }
                
                // CSV 분리 (따옴표 내 콤마 처리 등 복잡한 케이스는 미지원, 단순 콤마 분리)
                String[] values = line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                
                if (values.length < 1) continue;
                
                String stockCodeRaw = values[0].replaceAll("\"", "").trim();
                if (stockCodeRaw.isBlank()) continue;
                
                String stockCode = normalizeStockCode(stockCodeRaw);

                extractMetricsFromCsv(commands, values, rowIndex, stockCode, "ROA_IndRel", ROA_START_IDX);
                extractMetricsFromCsv(commands, values, rowIndex, stockCode, "DbRatio_IndRel", DEBT_START_IDX);
                extractMetricsFromCsv(commands, values, rowIndex, stockCode, "STDebtRatio_IndRel", SHORT_DEBT_START_IDX);
                extractMetricsFromCsv(commands, values, rowIndex, stockCode, "CurRatio_IndRel", CUR_RATIO_START_IDX);
                extractMetricsFromCsv(commands, values, rowIndex, stockCode, "CFO_AsRatio_IndRel", CFO_START_IDX);
                
                rowIndex++;
            }
        }
        return commands;
    }

    private void extractMetricsFromCsv(List<CompanyMetricValueCommand> commands, String[] values, int rowIndex, String stockCode, String metricCode, int startIdx) {
        for (int offset = 0; offset > -4; offset--) {
            int cellIndex = startIdx + Math.abs(offset);
            if (cellIndex >= values.length) continue;
            
            String cellValue = values[cellIndex].replaceAll("\"", "").trim();
            BigDecimal val = parseBigDecimal(cellValue);
            
            commands.add(new CompanyMetricValueCommand(
                    stockCode,
                    metricCode,
                    offset,
                    val,
                    rowIndex,
                    cellIndex,
                    null
            ));
        }
    }

    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.isBlank() || value.equals("-")) return null;
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String normalizeStockCode(String val) {
        if (val == null || val.isBlank()) return null;
        // 6자리 미만이면 앞에 0 채우기 (문자가 포함되어 있어도 적용)
        if (val.length() < 6) {
            return String.format("%6s", val).replace(' ', '0');
        }
        return val;
    }

    private void extractMetrics(List<CompanyMetricValueCommand> commands, Row row, int rowIndex, String stockCode, String metricCode, int startIdx) {
        for (int offset = 0; offset > -4; offset--) {
            int cellIndex = startIdx + Math.abs(offset);
            
            Cell cell = row.getCell(cellIndex);
            BigDecimal val = getBigDecimalValue(cell);
            
            // 빈 값(null)도 포함하여 명시적으로 적재되도록 함 (0은 null이 아니므로 포함됨)
            commands.add(new CompanyMetricValueCommand(
                    stockCode,
                    metricCode,
                    offset,
                    val,
                    rowIndex,
                    cellIndex,
                    null
            ));
        }
    }

    private String getStockCodeValue(Cell cell) {
        if (cell == null) return null;
        String val = null;
        if (cell.getCellType() == CellType.NUMERIC) {
            val = String.valueOf((long) cell.getNumericCellValue());
        } else if (cell.getCellType() == CellType.STRING) {
            val = cell.getStringCellValue().trim();
        }
        
        if (val == null || val.isBlank()) return null;
        
        // 6자리 미만이면 앞에 0 채우기 (문자가 포함되어 있어도 적용)
        if (val.length() < 6) {
            return String.format("%6s", val).replace(' ', '0');
        }
        return val;
    }

    private BigDecimal getBigDecimalValue(Cell cell) {
        if (cell == null) return null;
        try {
            return switch (cell.getCellType()) {
                case NUMERIC -> BigDecimal.valueOf(cell.getNumericCellValue());
                case STRING -> {
                    String s = cell.getStringCellValue().trim();
                    if (s.isEmpty() || s.equals("-")) yield null;
                    yield new BigDecimal(s);
                }
                default -> null;
            };
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
