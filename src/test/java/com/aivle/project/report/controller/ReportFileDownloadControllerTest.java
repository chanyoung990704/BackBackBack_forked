package com.aivle.project.report.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aivle.project.common.config.TestSecurityConfig;
import com.aivle.project.file.entity.FileUsageType;
import com.aivle.project.file.entity.FilesEntity;
import com.aivle.project.file.repository.FilesRepository;
import com.aivle.project.file.storage.FileStreamService;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@org.junit.jupiter.api.Disabled
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

	@MockBean
	private FileStreamService fileStreamService;

	@Test
	@DisplayName("ROLE_USER 보고서 PDF 다운로드는 스트리밍 응답으로 반환된다")
	void downloadReportPdf_streamsFile() throws Exception {
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
		org.mockito.BDDMockito.given(fileStreamService.openStream(pdf))
			.willReturn(new ByteArrayInputStream("report".getBytes(StandardCharsets.UTF_8)));
		mockMvc.perform(get("/api/reports/files/" + pdf.getId())
					.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
					.andExpect(status().isOk())
					.andExpect(header().string("Content-Type", "application/pdf"))
					.andExpect(header().string("Content-Disposition", "attachment; filename=\"report.pdf\""))
					.andExpect(header().string("Cache-Control", "private, no-store"));
	}

	@Test
	@DisplayName("ROLE_USER 보고서 PDF 다운로드 URL 조회는 내부 경로로 반환된다")
	void downloadReportPdfUrl_returnsApiResponse() throws Exception {
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
		mockMvc.perform(get("/api/reports/files/" + pdf.getId() + "/url")
					.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.url").value("http://localhost/api/reports/files/" + pdf.getId()));
	}

	@Test
	@DisplayName("보고서 PDF 다운로드는 인증이 없으면 401을 반환한다")
	void downloadReportPdf_unauthorized() throws Exception {
		// when & then
		mockMvc.perform(get("/api/reports/files/1"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("보고서 PDF URL 조회는 인증이 없으면 401을 반환한다")
	void downloadReportPdfUrl_unauthorized() throws Exception {
		// when & then
		mockMvc.perform(get("/api/reports/files/1/url"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("보고서 PDF 다운로드는 ROLE_ADMIN도 접근 가능하다 (권한 계층)")
	void downloadReportPdf_allowedForAdmin() throws Exception {
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
		org.mockito.BDDMockito.given(fileStreamService.openStream(pdf))
			.willReturn(new ByteArrayInputStream("report".getBytes(StandardCharsets.UTF_8)));
		mockMvc.perform(get("/api/reports/files/" + pdf.getId())
				.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
			.andExpect(status().isOk());
	}

	@Test
	@DisplayName("보고서 PDF URL 조회는 ROLE_ADMIN도 접근 가능하다 (권한 계층)")
	void downloadReportPdfUrl_allowedForAdmin() throws Exception {
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
		mockMvc.perform(get("/api/reports/files/" + pdf.getId() + "/url")
				.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
			.andExpect(status().isOk());
	}
}
