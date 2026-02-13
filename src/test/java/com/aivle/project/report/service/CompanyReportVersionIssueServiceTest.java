package com.aivle.project.report.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.aivle.project.company.entity.CompaniesEntity;
import com.aivle.project.file.entity.FileUsageType;
import com.aivle.project.file.entity.FilesEntity;
import com.aivle.project.quarter.entity.QuartersEntity;
import com.aivle.project.report.entity.CompanyReportVersionsEntity;
import com.aivle.project.report.entity.CompanyReportsEntity;
import com.aivle.project.report.repository.CompanyReportVersionsRepository;
import com.aivle.project.report.repository.CompanyReportsRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CompanyReportVersionIssueServiceTest {

	@InjectMocks
	private CompanyReportVersionIssueService companyReportVersionIssueService;

	@Mock
	private CompanyReportsRepository companyReportsRepository;

	@Mock
	private CompanyReportVersionsRepository companyReportVersionsRepository;

	@Test
	@DisplayName("리포트 ID가 있으면 행 잠금 후 다음 버전을 발급한다")
	void issueNextVersion_withReportId_locksAndIncrementsVersion() {
		// given
		CompanyReportsEntity report = newReportWithId(10L);
		CompanyReportsEntity lockedReport = newReportWithId(10L);
		FilesEntity pdfFile = FilesEntity.create(
			FileUsageType.REPORT_PDF,
			"http://localhost/files/report.pdf",
			"reports/report.pdf",
			"report.pdf",
			100L,
			"application/pdf"
		);
		CompanyReportVersionsEntity latest = CompanyReportVersionsEntity.create(
			lockedReport,
			3,
			java.time.LocalDateTime.now(),
			false,
			null
		);

		given(companyReportsRepository.findByIdForUpdate(10L)).willReturn(Optional.of(lockedReport));
		given(companyReportVersionsRepository.findTopByCompanyReportOrderByVersionNoDesc(lockedReport))
			.willReturn(Optional.of(latest));
		given(companyReportVersionsRepository.save(any(CompanyReportVersionsEntity.class)))
			.willAnswer(invocation -> invocation.getArgument(0));

		// when
		CompanyReportVersionsEntity issued = companyReportVersionIssueService.issueNextVersion(report, true, pdfFile);

		// then
		verify(companyReportsRepository).findByIdForUpdate(10L);
		assertThat(issued.getVersionNo()).isEqualTo(4);
		assertThat(issued.isPublished()).isTrue();
		assertThat(issued.getPdfFile()).isEqualTo(pdfFile);
	}

	@Test
	@DisplayName("리포트 ID가 없으면 잠금 없이 첫 버전을 발급한다")
	void issueNextVersion_withoutReportId_issuesInitialVersion() {
		// given
		CompanyReportsEntity report = CompanyReportsEntity.create(
			CompaniesEntity.create("00000001", "테스트기업", null, "000020", LocalDate.now()),
			QuartersEntity.create(2026, 1, 20261, LocalDate.now(), LocalDate.now()),
			null
		);
		given(companyReportVersionsRepository.findTopByCompanyReportOrderByVersionNoDesc(report))
			.willReturn(Optional.empty());
		given(companyReportVersionsRepository.save(any(CompanyReportVersionsEntity.class)))
			.willAnswer(invocation -> invocation.getArgument(0));

		// when
		CompanyReportVersionsEntity issued = companyReportVersionIssueService.issueNextVersion(report, false, null);

		// then
		verify(companyReportsRepository, never()).findByIdForUpdate(any());
		assertThat(issued.getVersionNo()).isEqualTo(1);
		assertThat(issued.isPublished()).isFalse();
		assertThat(issued.getPdfFile()).isNull();
	}

	private CompanyReportsEntity newReportWithId(Long id) {
		CompanyReportsEntity report = CompanyReportsEntity.create(
			CompaniesEntity.create("00000001", "테스트기업", null, "000020", LocalDate.now()),
			QuartersEntity.create(2026, 1, 20261, LocalDate.now(), LocalDate.now()),
			null
		);
		ReflectionTestUtils.setField(report, "id", id);
		return report;
	}
}
