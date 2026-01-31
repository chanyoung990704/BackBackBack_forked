package com.aivle.project.report.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aivle.project.common.config.TestSecurityConfig;
import com.aivle.project.file.entity.FileUsageType;
import com.aivle.project.file.entity.FilesEntity;
import com.aivle.project.file.repository.FilesRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
@Transactional
class ReportFileDownloadControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private FilesRepository filesRepository;

	@Test
	@DisplayName("관리자 보고서 PDF 다운로드는 외부 URL로 리다이렉트된다")
	void downloadReportPdf_redirectsToStorageUrl() throws Exception {
		// given
		FilesEntity pdf = filesRepository.save(FilesEntity.create(
			FileUsageType.REPORT_PDF,
			"http://example.com/report.pdf",
			null,
			"report.pdf",
			1200L,
			"application/pdf"
		));

		// when & then
		mockMvc.perform(get("/admin/reports/files/" + pdf.getId())
				.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
			.andExpect(status().isFound())
			.andExpect(header().string("Location", "http://example.com/report.pdf"));
	}
}
