package com.aivle.project.company.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.aivle.project.company.entity.CompaniesEntity;
import com.aivle.project.company.repository.CompaniesRepository;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CompanyAiReportStoreServiceTest {

	@InjectMocks
	private CompanyAiReportStoreService service;

	@Mock
	private CompaniesRepository companiesRepository;
	@Mock
	private FileStorageService fileStorageService;
	@Mock
	private FilesRepository filesRepository;
	@Mock
	private QuartersRepository quartersRepository;
	@Mock
	private CompanyReportsRepository companyReportsRepository;
	@Mock
	private CompanyReportVersionIssueService companyReportVersionIssueService;

	@Test
	@DisplayName("AI PDF 저장 시 파일 메타데이터 저장 후 보고서 버전 발급까지 연결한다")
	void storeAndLinkReport_shouldStoreFileAndIssueVersion() {
		Long companyId = 1L;
		int year = 2026;
		int quarter = 1;
		byte[] content = "pdf".getBytes();

		CompaniesEntity company = CompaniesEntity.create("00000001", "삼성전자", null, "005930", LocalDate.now());
		QuartersEntity quarterEntity = QuartersEntity.create(year, quarter, 20261, LocalDate.now(), LocalDate.now());
		CompanyReportsEntity report = CompanyReportsEntity.create(company, quarterEntity, null);
		ReflectionTestUtils.setField(company, "id", companyId);
		ReflectionTestUtils.setField(quarterEntity, "id", 10L);
		StoredFile stored = new StoredFile(
			"https://example.com/report.pdf",
			"report.pdf",
			123L,
			"application/pdf",
			"reports/005930/2026/1/report.pdf"
		);
		FilesEntity savedFile = FilesEntity.create(
			com.aivle.project.file.entity.FileUsageType.REPORT_PDF,
			stored.storageUrl(),
			stored.storageKey(),
			stored.originalFilename(),
			stored.fileSize(),
			stored.contentType()
		);

		given(companiesRepository.findById(companyId)).willReturn(Optional.of(company));
		given(fileStorageService.store(any(), eq("reports/005930/2026/1"))).willReturn(stored);
		given(filesRepository.save(any(FilesEntity.class))).willReturn(savedFile);
		given(quartersRepository.findByYearAndQuarter((short) year, (byte) quarter)).willReturn(Optional.of(quarterEntity));
		given(companyReportsRepository.findByCompanyIdAndQuarterId(companyId, quarterEntity.getId())).willReturn(Optional.of(report));
		given(companyReportVersionIssueService.issueNextVersion(eq(report), eq(true), eq(savedFile)))
			.willReturn(CompanyReportVersionsEntity.create(report, 1, LocalDateTime.now(), true, savedFile));

		FilesEntity result = service.storeAndLinkReport(companyId, year, quarter, content);

		assertThat(result).isEqualTo(savedFile);
		verify(companyReportVersionIssueService).issueNextVersion(eq(report), eq(true), eq(savedFile));
	}
}
