package com.aivle.project.file.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aivle.project.category.entity.CategoriesEntity;
import com.aivle.project.common.config.TestSecurityConfig;
import com.aivle.project.file.entity.FilesEntity;
import com.aivle.project.file.entity.PostFilesEntity;
import com.aivle.project.file.repository.PostFilesRepository;
import com.aivle.project.post.entity.PostStatus;
import com.aivle.project.post.entity.PostsEntity;
import com.aivle.project.user.entity.UserEntity;
import com.aivle.project.user.entity.UserStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(TestSecurityConfig.class)
class FileControllerIntegrationTest {

	@TempDir
	static Path tempDir;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PostFilesRepository postFilesRepository;

	@PersistenceContext
	private EntityManager entityManager;

	@DynamicPropertySource
	static void registerProperties(DynamicPropertyRegistry registry) {
		registry.add("app.file.upload.upload-dir", () -> tempDir.toString());
	}

	@Test
	@DisplayName("게시글 파일 업로드 시 메타데이터가 저장된다")
	void upload_shouldPersistFiles() throws Exception {
		// given
		UserEntity user = persistUser("file-upload@test.com");
		CategoriesEntity category = persistCategory("files");
		PostsEntity post = persistPost(user, category, "제목", "내용");

		MockMultipartFile first = new MockMultipartFile(
			"files",
			"a.png",
			"image/png",
			"hello".getBytes(StandardCharsets.UTF_8)
		);
		MockMultipartFile second = new MockMultipartFile(
			"files",
			"b.png",
			"image/png",
			"world".getBytes(StandardCharsets.UTF_8)
		);

		// when
		mockMvc.perform(multipart("/api/posts/{postId}/files", post.getId())
				.file(first)
				.file(second)
				.with(jwt().jwt(jwt -> jwt.subject(user.getUuid().toString()))))
			.andExpect(status().isCreated());

		// then
		List<PostFilesEntity> saved = postFilesRepository.findAllActiveByPostIdOrderByCreatedAtAsc(post.getId());
		assertThat(saved).hasSize(2);

		for (PostFilesEntity mapping : saved) {
			FilesEntity file = mapping.getFile();
			assertThat(file.getStorageUrl()).startsWith("memory://posts/");
			assertThat(file.getStorageKey()).startsWith("posts/");
		}
	}

	private UserEntity persistUser(String email) {
		UserEntity user = newEntity(UserEntity.class);
		ReflectionTestUtils.setField(user, "email", email);
		ReflectionTestUtils.setField(user, "password", "encoded-password");
		ReflectionTestUtils.setField(user, "name", "test-user");
		ReflectionTestUtils.setField(user, "status", UserStatus.ACTIVE);
		entityManager.persist(user);
		entityManager.flush();
		return user;
	}

	private CategoriesEntity persistCategory(String name) {
		CategoriesEntity category = newEntity(CategoriesEntity.class);
		ReflectionTestUtils.setField(category, "name", name);
		entityManager.persist(category);
		entityManager.flush();
		return category;
	}

	private PostsEntity persistPost(UserEntity user, CategoriesEntity category, String title, String content) {
		PostsEntity post = PostsEntity.create(
			user,
			category,
			title,
			content,
			false,
			PostStatus.PUBLISHED
		);
		entityManager.persist(post);
		entityManager.flush();
		return post;
	}

	private <T> T newEntity(Class<T> type) {
		try {
			var ctor = type.getDeclaredConstructor();
			ctor.setAccessible(true);
			return ctor.newInstance();
		} catch (ReflectiveOperationException ex) {
			throw new IllegalStateException("엔티티 생성에 실패했습니다", ex);
		}
	}
}
