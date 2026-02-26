package com.aivle.project.company.service;

import com.aivle.project.common.util.GetOrCreateResolver;
import com.aivle.project.common.util.SimpleMultipartFile;
import com.aivle.project.company.entity.CompaniesEntity;
import com.aivle.project.company.repository.CompaniesRepository;
import com.aivle.project.file.entity.FileUsageType;
import com.aivle.project.file.entity.FilesEntity;
import com.aivle.project.file.repository.FilesRepository;
import com.aivle.project.file.storage.FileStorageService;
import com.aivle.project.file.storage.StoredFile;
import com.aivle.project.quarter.entity.QuartersEntity;
import com.aivle.project.quarter.repository.QuartersRepository;
import com.aivle.project.report.entity.CompanyReportVersionsEntity;
import com.aivle.project.report.entity.CompanyReportsEntity;
import com.aivle.project.report.repository.CompanyReportsRepository;
import com.aivle.project.report.service.CompanyReportVersionIssueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

/**
 * AI 보고서 PDF의 스토리지 및 DB 저장을 독립된 트랜잭션으로 처리하는 서비스.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyAiReportStoreService {

    private final CompaniesRepository companiesRepository;
    private final FileStorageService fileStorageService;
    private final FilesRepository filesRepository;
    private final QuartersRepository quartersRepository;
    private final CompanyReportsRepository companyReportsRepository;
    private final CompanyReportVersionIssueService companyReportVersionIssueService;

    /**
     * 다운로드된 PDF 내용을 스토리지와 DB에 저장하고 보고서 버전에 연결합니다.
     */
    @Transactional
    public FilesEntity storeAndLinkReport(Long companyId, int targetYear, int targetQuarter, byte[] pdfContent) {
        CompaniesEntity company = companiesRepository.findById(companyId)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 기업 ID입니다: " + companyId));

        // 4. MultipartFile 생성
        String filename = String.format("report_%s_%d_%d_%s.pdf", company.getStockCode(), targetYear, targetQuarter, LocalDate.now());
        MultipartFile multipartFile = new SimpleMultipartFile(
            "file",
            filename,
            "application/pdf",
            pdfContent
        );

        // 5. 파일 저장 (로컬 또는 S3)
        String subDir = String.format("reports/%s/%d/%d", company.getStockCode(), targetYear, targetQuarter);
        StoredFile storedFile = fileStorageService.store(multipartFile, subDir);

        // 6. DB에 파일 메타데이터 저장
        FilesEntity filesEntity = FilesEntity.create(
            FileUsageType.REPORT_PDF,
            storedFile.storageUrl(),
            storedFile.storageKey(),
            storedFile.originalFilename(),
            storedFile.fileSize(),
            storedFile.contentType()
        );
        FilesEntity savedFileEntity = filesRepository.save(filesEntity);

        // 7. 분기 조회 또는 생성
        QuartersEntity quarterEntity = getOrCreateQuarter(targetYear, targetQuarter);

        // 8. 기업-분기 보고서 조회 또는 생성
        CompanyReportsEntity report = getOrCreateReport(company, quarterEntity);

        // 9. 새 버전 등록
        CompanyReportVersionsEntity version = companyReportVersionIssueService.issueNextVersion(report, true, savedFileEntity);
        log.info("Linked AI report to company_report_versions (ID: {}, Year: {}, Quarter: {}, Version: {})",
            report.getId(), targetYear, targetQuarter, version.getVersionNo());
        
        return savedFileEntity;
    }

    private QuartersEntity createNewQuarter(int year, int quarter) {
        log.info("Creating new quarter: {} year {} quarter", year, quarter);
        int quarterKey = year * 10 + quarter;
        LocalDate startDate = LocalDate.of(year, (quarter - 1) * 3 + 1, 1);
        LocalDate endDate = startDate.plusMonths(3).minusDays(1);

        QuartersEntity newQuarter = QuartersEntity.create(year, quarter, quarterKey, startDate, endDate);
        return quartersRepository.save(newQuarter);
    }

    private QuartersEntity getOrCreateQuarter(int year, int quarter) {
        return GetOrCreateResolver.resolve(
            () -> quartersRepository.findByYearAndQuarter((short) year, (byte) quarter),
            () -> createNewQuarter(year, quarter),
            () -> quartersRepository.findByYearAndQuarter((short) year, (byte) quarter)
        );
    }

    private CompanyReportsEntity getOrCreateReport(CompaniesEntity company, QuartersEntity quarter) {
        return GetOrCreateResolver.resolve(
            () -> companyReportsRepository.findByCompanyIdAndQuarterId(company.getId(), quarter.getId()),
            () -> companyReportsRepository.save(CompanyReportsEntity.create(company, quarter, null)),
            () -> companyReportsRepository.findByCompanyIdAndQuarterId(company.getId(), quarter.getId())
        );
    }
}
