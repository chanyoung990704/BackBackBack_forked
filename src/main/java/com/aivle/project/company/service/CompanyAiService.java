package com.aivle.project.company.service;

import com.aivle.project.common.util.SimpleMultipartFile;
import com.aivle.project.company.client.AiServerClient;
import com.aivle.project.company.dto.AiAnalysisResponse;
import com.aivle.project.file.entity.FileUsageType;
import com.aivle.project.file.entity.FilesEntity;
import com.aivle.project.file.repository.FilesRepository;
import com.aivle.project.file.storage.FileStorageService;
import com.aivle.project.file.storage.StoredFile;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyAiService {

    private final AiServerClient aiServerClient;
    private final FileStorageService fileStorageService;
    private final FilesRepository filesRepository;

    /**
     * 특정 기업의 AI 재무 분석 예측 결과를 조회합니다.
     * @param companyCode 기업 코드 (예: 005930)
     * @return 예측 결과 DTO
     */
    public AiAnalysisResponse getCompanyAnalysis(String companyCode) {
        log.info("Fetching AI analysis for companyCode: {}", companyCode);
        return aiServerClient.getPrediction(companyCode);
    }

    /**
     * AI 서버에서 분석 리포트(PDF)를 생성하고 파일 서버에 저장합니다.
     * @param companyCode 기업 코드
     * @return 저장된 파일 엔티티
     */
    @Transactional
    public FilesEntity generateAndSaveReport(String companyCode) {
        log.info("Generating and saving AI report for companyCode: {}", companyCode);

        // 1. AI 서버에서 PDF 다운로드
        byte[] pdfContent = aiServerClient.getAnalysisReportPdf(companyCode);

        // 2. MultipartFile 생성
        String filename = String.format("report_%s_%s.pdf", companyCode, LocalDate.now());
        MultipartFile multipartFile = new SimpleMultipartFile(
            "file",
            filename,
            "application/pdf",
            pdfContent
        );

        // 3. 파일 저장 (로컬 또는 S3)
        // reports 폴더 하위에 저장
        StoredFile storedFile = fileStorageService.store(multipartFile, "reports");

        // 4. DB에 메타데이터 저장
        FilesEntity filesEntity = FilesEntity.create(
            FileUsageType.REPORT_PDF, // FileUsageType에 REPORT_PDF가 존재한다고 가정
            storedFile.storageUrl(),
            storedFile.storageKey(),
            storedFile.originalFilename(),
            storedFile.fileSize(),
            storedFile.contentType()
        );

        return filesRepository.save(filesEntity);
    }
}
