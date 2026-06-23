package com.aivle.project.company.report.importer;

import com.aivle.project.company.report.dto.CompanyMetricValueCommand;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * 엑셀 지표 파일 파서.
 */
@Slf4j
@Component
public class ExcelMetricParser {

	private static final String HEADER_STOCK_CODE = "기업코드";
	private static final String HEADER_COMPANY_NAME = "기업명";
	private static final String SHEET_NAME = "in";

	public List<CompanyMetricValueCommand> parse(Path filePath) throws IOException {
		try (InputStream inputStream = Files.newInputStream(filePath)) {
			return parse(inputStream);
		}
	}

	public List<CompanyMetricValueCommand> parse(MultipartFile file) throws IOException {
		try (InputStream inputStream = file.getInputStream()) {
			return parse(inputStream);
		}
	}

	public List<CompanyMetricValueCommand> parse(InputStream inputStream) throws IOException {
		try (Workbook workbook = WorkbookFactory.create(inputStream)) {
			Sheet sheet = workbook.getSheet(SHEET_NAME);
			if (sheet == null) {
				sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
			}
			if (sheet == null) {
				return List.of();
			}

			Row headerRow = sheet.getRow(0);
			if (headerRow == null) {
				return List.of();
			}

			HeaderContext headerContext = parseHeaders(headerRow);
			if (headerContext.stockCodeIndex < 0) {
				log.info("엑셀 파싱 실패: 기업코드 헤더 없음");
				return List.of();
			}

			DataFormatter formatter = new DataFormatter();
			List<CompanyMetricValueCommand> commands = new ArrayList<>();

			for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
				Row row = sheet.getRow(rowIndex);
				if (row == null) {
					continue;
				}

				String stockCode = readCellAsString(row.getCell(headerContext.stockCodeIndex), formatter);
				if (stockCode.isBlank()) {
					log.info("엑셀 파싱 스킵: 기업코드 없음 (row={})", rowIndex + 1);
					continue;
				}

				for (Map.Entry<Integer, HeaderSpec> entry : headerContext.metricHeaders.entrySet()) {
					Cell cell = row.getCell(entry.getKey());
					BigDecimal value = parseNumeric(cell, formatter);
					HeaderSpec spec = entry.getValue();
					int excelRow = rowIndex + 1;
					int excelCol = entry.getKey() + 1;
					commands.add(new CompanyMetricValueCommand(
						stockCode,
						spec.metricCode(),
						spec.quarterOffset(),
						value,
						excelRow,
						excelCol,
						spec.headerName()
					));
					log.info(
						"엑셀 파싱: row={}, col={}, stockCode={}, header={}, metricCode={}, offset={}, value={}",
						excelRow,
						excelCol,
						stockCode,
						spec.headerName(),
						spec.metricCode(),
						spec.quarterOffset(),
						value
					);
				}
			}

			return commands;
		}
	}

	private HeaderContext parseHeaders(Row headerRow) {
		Map<Integer, HeaderSpec> metricHeaders = new HashMap<>();
		int stockCodeIndex = -1;

		for (Cell cell : headerRow) {
			String header = readCellAsString(cell, new DataFormatter());
			if (HEADER_STOCK_CODE.equals(header)) {
				stockCodeIndex = cell.getColumnIndex();
				continue;
			}
			if (HEADER_COMPANY_NAME.equals(header)) {
				continue;
			}
			HeaderSpec spec = parseMetricHeader(header);
			if (spec != null) {
				metricHeaders.put(cell.getColumnIndex(), spec);
			} else if (!header.isBlank()) {
				log.info("엑셀 파싱 스킵: 헤더 매핑 실패 (col={}, header={})", cell.getColumnIndex() + 1, header);
			}
		}

		return new HeaderContext(stockCodeIndex, metricHeaders);
	}

	private HeaderSpec parseMetricHeader(String header) {
		if (header == null || header.isBlank()) {
			return null;
		}
		int delimiterIndex = header.lastIndexOf('_');
		if (delimiterIndex < 0) {
			return null;
		}
		String metricName = header.substring(0, delimiterIndex);
		String suffix = header.substring(delimiterIndex + 1);

		String metricCode = MetricHeaderMapping.toMetricCode(metricName);
		if (metricCode == null) {
			log.info("엑셀 파싱 스킵: 지표 매핑 없음 (header={})", header);
			return null;
		}

		Integer offset = parseQuarterOffset(suffix);
		if (offset == null) {
			log.info("엑셀 파싱 스킵: 분기 접미사 인식 실패 (header={})", header);
			return null;
		}

		return new HeaderSpec(metricCode, offset, header);
	}

	private Integer parseQuarterOffset(String suffix) {
		if ("현재".equals(suffix)) {
			return 0;
		}
		if (suffix != null && suffix.startsWith("분기-")) {
			String number = suffix.substring(3);
			try {
				int offset = Integer.parseInt(number);
				return -offset;
			} catch (NumberFormatException ignored) {
				return null;
			}
		}
		return null;
	}

	private String readCellAsString(Cell cell, DataFormatter formatter) {
		if (cell == null) {
			return "";
		}
		String value = formatter.formatCellValue(cell);
		return value == null ? "" : value.trim();
	}

	private BigDecimal parseNumeric(Cell cell, DataFormatter formatter) {
		String raw = readCellAsString(cell, formatter);
		if (raw.isBlank()) {
			return null;
		}
		String normalized = raw.replace(",", "").replace("%", "").trim();
		if (normalized.isBlank()) {
			return null;
		}
		try {
			return new BigDecimal(normalized);
		} catch (NumberFormatException ex) {
			log.info("엑셀 파싱 스킵: 숫자 변환 실패 (value={})", raw);
			return null;
		}
	}

	private record HeaderSpec(String metricCode, int quarterOffset, String headerName) {
	}

	private record HeaderContext(int stockCodeIndex, Map<Integer, HeaderSpec> metricHeaders) {
	}
}
