package com.aivle.project.company.report.importer;

import static org.assertj.core.api.Assertions.assertThat;

import com.aivle.project.company.report.dto.CompanyMetricValueCommand;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ExcelMetricParserTest {

	private final ExcelMetricParser parser = new ExcelMetricParser();

	@Test
	@DisplayName("엑셀 파일에서 지표 명령을 파싱한다")
	void parseExcel() throws Exception {
		// given
		byte[] bytes = buildExcel();

		// when
		List<CompanyMetricValueCommand> commands = parser.parse(new ByteArrayInputStream(bytes));

		// then
		assertThat(commands).isNotEmpty();
		assertThat(commands).anyMatch(command ->
			"20".equals(command.stockCode())
				&& "ROA".equals(command.metricCode())
				&& command.quarterOffset() == 0
				&& command.metricValue() != null
				&& command.rowIndex() > 1
				&& command.colIndex() > 1
		);
		assertThat(commands).anyMatch(command ->
			"20".equals(command.stockCode())
				&& "OpMargin".equals(command.metricCode())
				&& command.quarterOffset() == -3
				&& "매출액영업이익률_분기-3".equals(command.headerName())
		);
	}

	private byte[] buildExcel() throws Exception {
		try (Workbook workbook = new XSSFWorkbook()) {
			Sheet sheet = workbook.createSheet("input");
			Row header = sheet.createRow(0);
			header.createCell(0).setCellValue("기업코드");
			header.createCell(1).setCellValue("ROA_현재");
			header.createCell(2).setCellValue("매출액영업이익률_분기-3");

			Row row = sheet.createRow(1);
			row.createCell(0).setCellValue("20");
			row.createCell(1).setCellValue(1.23);
			row.createCell(2).setCellValue(9.87);

			try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
				workbook.write(outputStream);
				return outputStream.toByteArray();
			}
		}
	}
}
