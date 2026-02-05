package com.aivle.project.company.dto;

import com.aivle.project.file.entity.FilesEntity;

/**
 * AI 리포트 파일 저장 응답 DTO.
 */
public record AiReportFileResponse(
    Long fileId,
    String storageUrl,
    String originalFilename,
    long fileSize,
    String contentType
) {

    public static AiReportFileResponse from(FilesEntity file) {
        return new AiReportFileResponse(
            file.getId(),
            file.getStorageUrl(),
            file.getOriginalFilename(),
            file.getFileSize(),
            file.getContentType()
        );
    }
}
