package com.aivle.project.company.service;

import com.aivle.project.common.util.SimpleMultipartFile;
import com.aivle.project.company.client.AiServerClient;
import com.aivle.project.file.entity.FileUsageType;
import com.aivle.project.file.entity.FilesEntity;
import com.aivle.project.file.repository.FilesRepository;
import com.aivle.project.file.storage.FileStorageService;
import com.aivle.project.file.storage.StoredFile;
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

    @Test
    @DisplayName("AI 리포트를 생성하고 저장한다")
    void generateAndSaveReport_Success() {
        // given
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

        given(aiServerClient.getAnalysisReportPdf(companyCode)).willReturn(pdfContent);
        given(fileStorageService.store(any(MultipartFile.class), eq("reports"))).willReturn(storedFile);
        given(filesRepository.save(any(FilesEntity.class))).willReturn(savedEntity);

        // when
        FilesEntity result = companyAiService.generateAndSaveReport(companyCode);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getStorageUrl()).isEqualTo(storedFile.storageUrl());

        verify(aiServerClient).getAnalysisReportPdf(companyCode);
        verify(fileStorageService).store(any(SimpleMultipartFile.class), eq("reports"));
        verify(filesRepository).save(any(FilesEntity.class));
    }
}
