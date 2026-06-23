package com.aivle.project.company.report.mapper;

import com.aivle.project.company.report.dto.ReportMetricItemDto;
import com.aivle.project.company.report.dto.ReportMetricGroupedResponse;
import com.aivle.project.company.report.dto.ReportMetricQuarterGroupDto;
import com.aivle.project.company.report.dto.ReportMetricRowDto;
import com.aivle.project.company.report.dto.ReportMetricRowProjection;
import com.aivle.project.company.report.dto.ReportLatestPredictResponse;
import com.aivle.project.company.report.dto.ReportPredictMetricItemDto;
import com.aivle.project.company.report.dto.ReportPredictMetricRowProjection;
import java.time.LocalDateTime;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ReportMapper {

	// 조회 결과(projection)를 API 응답 DTO로 변환한다.
	ReportMetricRowDto toRowDto(ReportMetricRowProjection projection);

	ReportMetricItemDto toItemDto(ReportMetricRowDto row);

	// 예측 값 전용 projection을 응답 아이템으로 변환한다.
	ReportPredictMetricItemDto toPredictItemDto(ReportPredictMetricRowProjection projection);

	// 그룹핑 응답은 필수 필드만 전달받아 응답 DTO로 조립한다.
	ReportMetricGroupedResponse toGroupedResponse(
		String corpName,
		String stockCode,
		int fromQuarterKey,
		int toQuarterKey,
		List<ReportMetricQuarterGroupDto> quarters
	);

	// 최신 예측 응답은 배치/파일 정보를 포함해 하나의 DTO로 조립한다.
	ReportLatestPredictResponse toLatestPredictResponse(
		String corpName,
		String stockCode,
		int quarterKey,
		Integer versionNo,
		LocalDateTime generatedAt,
		Long pdfFileId,
		String pdfFileName,
		String pdfContentType,
		String pdfDownloadUrl,
		List<ReportPredictMetricItemDto> metrics
	);
}
