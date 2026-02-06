package com.aivle.project.report.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.aivle.project.company.entity.CompaniesEntity;
import com.aivle.project.company.repository.CompaniesRepository;
import com.aivle.project.common.config.TestSecurityConfig;
import com.aivle.project.file.repository.FilesRepository;
import com.aivle.project.report.dto.ReportPdfPublishResult;
import com.aivle.project.report.repository.CompanyReportMetricValuesRepository;
import com.aivle.project.report.repository.CompanyReportVersionsRepository;
import com.aivle.project.report.repository.CompanyReportsRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
@Transactional
class CompanyReportPdfPublishServiceTest {

	@Autowired
	private CompanyReportPdfPublishService companyReportPdfPublishService;

	@Autowired
	private CompaniesRepository companiesRepository;

	@Autowired
	private CompanyReportsRepository companyReportsRepository;

	@Autowired
	private CompanyReportVersionsRepository companyReportVersionsRepository;

	@Autowired
	private CompanyReportMetricValuesRepository companyReportMetricValuesRepository;

	@Autowired
	private FilesRepository filesRepository;

	@Test
	@DisplayName("PDF만 업로드하면 새 버전으로 발행 처리한다")
	void publishPdfOnly_savesPdfAndPublishesVersion() {
		// given
		companiesRepository.save(CompaniesEntity.create(
			"00000001",
			"테스트기업",
			"TEST_CO",
			"000020",
			LocalDate.of(2025, 1, 1)
		));
		MockMultipartFile pdf = new MockMultipartFile(
			"file",
			"report.pdf",
			"application/pdf",
			"%PDF-1.4".getBytes()
		);

		// when
		ReportPdfPublishResult result = companyReportPdfPublishService.publishPdfOnly("000020", 20253, pdf);

		// then
		assertThat(result.saved()).isEqualTo(1);
		assertThat(result.skippedCompanies()).isEqualTo(0);
		assertThat(result.reportVersionNo()).isEqualTo(1);
		assertThat(result.pdfFileId()).isNotNull();
		assertThat(companyReportsRepository.count()).isEqualTo(1);
		assertThat(companyReportVersionsRepository.count()).isEqualTo(1);
		assertThat(companyReportMetricValuesRepository.count()).isEqualTo(0);
		assertThat(filesRepository.count()).isEqualTo(1);
		assertThat(companyReportVersionsRepository.findAll().get(0).isPublished()).isTrue();
	}

	@Test
	@DisplayName("기업이 없으면 PDF 업로드를 스킵한다")
	void publishPdfOnly_skipsWhenCompanyMissing() {
		// given
		MockMultipartFile pdf = new MockMultipartFile(
			"file",
			"report.pdf",
			"application/pdf",
			"%PDF-1.4".getBytes()
		);

		// when
		ReportPdfPublishResult result = companyReportPdfPublishService.publishPdfOnly("000020", 20253, pdf);

		// then
		assertThat(result.saved()).isEqualTo(0);
		assertThat(result.skippedCompanies()).isEqualTo(1);
		assertThat(result.reportVersionNo()).isNull();
		assertThat(result.pdfFileId()).isNull();
		assertThat(companyReportsRepository.count()).isZero();
		assertThat(companyReportVersionsRepository.count()).isZero();
		assertThat(filesRepository.count()).isZero();
	}
}
