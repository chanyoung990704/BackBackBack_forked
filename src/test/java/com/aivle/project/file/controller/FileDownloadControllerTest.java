package com.aivle.project.file.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aivle.project.file.entity.FileUsageType;
import com.aivle.project.file.entity.FilesEntity;
import com.aivle.project.file.service.FileService;
import com.aivle.project.file.storage.FileDownloadUrlResolver;
import com.aivle.project.file.storage.FileStreamService;
import com.aivle.project.user.entity.UserEntity;
import com.aivle.project.user.entity.UserStatus;
import com.aivle.project.user.repository.UserRepository;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import com.aivle.project.common.security.CurrentUserArgumentResolver;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;

@WebMvcTest(
	controllers = FileDownloadController.class,
	properties = {
		"spring.autoconfigure.exclude="
			+ "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
			+ "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
			+ "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration,"
			+ "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"
	}
)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class FileDownloadControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@org.springframework.boot.test.mock.mockito.MockBean
	private FileService fileService;

	@org.springframework.boot.test.mock.mockito.MockBean
	private FileDownloadUrlResolver fileDownloadUrlResolver;

	@org.springframework.boot.test.mock.mockito.MockBean
	private FileStreamService fileStreamService;

	@org.springframework.boot.test.mock.mockito.MockBean
	private UserRepository userRepository;

	@org.springframework.boot.test.mock.mockito.MockBean
	private JpaMetamodelMappingContext jpaMetamodelMappingContext;

	@org.springframework.boot.test.mock.mockito.MockBean
	private CurrentUserArgumentResolver currentUserArgumentResolver;

	@Test
	@DisplayName("다운로드 요청 시 파일을 스트리밍으로 응답한다")
	void download_shouldStreamFile() throws Exception {
		// given
		UserEntity user = newUser(1L);
		FilesEntity file = FilesEntity.create(
			FileUsageType.POST_ATTACHMENT,
			"https://bucket.s3.ap-northeast-2.amazonaws.com/uploads/a.pdf",
			"uploads/a.pdf",
			"a.pdf",
			100L,
			"application/pdf"
		);

		given(currentUserArgumentResolver.supportsParameter(any())).willReturn(true);
		given(currentUserArgumentResolver.resolveArgument(any(), any(), any(), any())).willReturn(user);
		given(fileService.getFile(any(Long.class), any(UserEntity.class))).willReturn(file);
		given(fileStreamService.openStream(any(FilesEntity.class)))
			.willReturn(new ByteArrayInputStream("data".getBytes(StandardCharsets.UTF_8)));

		// when & then
		mockMvc.perform(get("/api/files/1"))
			.andExpect(status().isOk())
			.andExpect(header().string("Content-Type", "application/pdf"))
			.andExpect(header().string("Content-Disposition", "attachment; filename=\"a.pdf\""))
			.andExpect(header().string("Cache-Control", "private, no-store"));
	}

	@Test
	@DisplayName("다운로드 URL 조회 시 내부 다운로드 URL을 반환한다")
	void downloadUrl_shouldReturnInternalUrl() throws Exception {
		// given
		UserEntity user = newUser(1L);
		FilesEntity file = FilesEntity.create(
			FileUsageType.POST_ATTACHMENT,
			"https://bucket.s3.ap-northeast-2.amazonaws.com/uploads/a.pdf",
			"uploads/a.pdf",
			"a.pdf",
			100L,
			"application/pdf"
		);

		given(currentUserArgumentResolver.supportsParameter(any())).willReturn(true);
		given(currentUserArgumentResolver.resolveArgument(any(), any(), any(), any())).willReturn(user);
		given(fileService.getFile(any(Long.class), any(UserEntity.class))).willReturn(file);

		// when & then
		mockMvc.perform(get("/api/files/1/url"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.url").value("http://localhost/api/files/1"));
	}

	@Test
	@DisplayName("신규 다운로드 URL 경로(/download-url)로 내부 다운로드 URL을 반환한다")
	void downloadUrlNewPath_shouldReturnInternalUrl() throws Exception {
		// given
		UserEntity user = newUser(1L);
		FilesEntity file = FilesEntity.create(
			FileUsageType.POST_ATTACHMENT,
			"https://bucket.s3.ap-northeast-2.amazonaws.com/uploads/a.pdf",
			"uploads/a.pdf",
			"a.pdf",
			100L,
			"application/pdf"
		);

		given(currentUserArgumentResolver.supportsParameter(any())).willReturn(true);
		given(currentUserArgumentResolver.resolveArgument(any(), any(), any(), any())).willReturn(user);
		given(fileService.getFile(any(Long.class), any(UserEntity.class))).willReturn(file);

		// when & then
		mockMvc.perform(get("/api/files/1/download-url"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.url").value("http://localhost/api/files/1"));
	}

	private static UserEntity newUser(Long id) {
		try {
			var ctor = UserEntity.class.getDeclaredConstructor();
			ctor.setAccessible(true);
			UserEntity user = ctor.newInstance();
			ReflectionTestUtils.setField(user, "id", id);
			ReflectionTestUtils.setField(user, "status", UserStatus.ACTIVE);
			return user;
		} catch (ReflectiveOperationException ex) {
			throw new IllegalStateException("UserEntity 생성에 실패했습니다", ex);
		}
	}
}
