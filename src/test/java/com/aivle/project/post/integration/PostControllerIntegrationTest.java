package com.aivle.project.post.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aivle.project.category.entity.CategoriesEntity;
import com.aivle.project.common.config.TestSecurityConfig;
import com.aivle.project.common.dto.ApiResponse;
import com.aivle.project.common.dto.PageResponse;
import com.aivle.project.post.dto.PostCreateRequest;
import com.aivle.project.post.dto.PostResponse;
import com.aivle.project.post.dto.PostUpdateRequest;
import com.aivle.project.post.entity.PostStatus;
import com.aivle.project.post.entity.PostsEntity;
import com.aivle.project.user.entity.UserEntity;
import com.aivle.project.user.entity.UserStatus;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
@Import(TestSecurityConfig.class)
class PostControllerIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@PersistenceContext
	private EntityManager entityManager;

	@Test
	@DisplayName("페이징 파라미터로 게시글 목록을 조회한다")
	void list_shouldReturnPagedPosts() throws Exception {
		// given: 사용자/카테고리와 게시글 12건을 준비
		UserEntity user = persistUser("post-page@test.com");
		CategoriesEntity category = persistCategory("page");
		for (int i = 0; i < 12; i++) {
			persistPost(user, category, "제목-" + i, "내용-" + i);
		}

		// when: 2페이지(5개씩) 조회
		MvcResult result = mockMvc.perform(get("/posts")
				.param("page", "2")
				.param("size", "5")
				.param("sortBy", "createdAt")
				.param("direction", "DESC")
				.with(jwt().jwt(jwt -> jwt.subject(user.getUuid().toString()))))
			.andExpect(status().isOk())
			.andReturn();

		// then: 페이지 응답 메타가 반환된다
		ApiResponse<PageResponse<PostResponse>> apiResponse = objectMapper.readValue(
			result.getResponse().getContentAsString(),
			new TypeReference<ApiResponse<PageResponse<PostResponse>>>() {}
		);
		PageResponse<PostResponse> page = apiResponse.data();

		assertThat(apiResponse.success()).isTrue();
		assertThat(page.page()).isEqualTo(2);
		assertThat(page.size()).isEqualTo(5);
		assertThat(page.totalElements()).isEqualTo(12);
		assertThat(page.totalPages()).isEqualTo(3);
		assertThat(page.last()).isFalse();
		assertThat(page.content()).hasSize(5);
	}

	@Test
	@DisplayName("JWT subject 기반으로 게시글을 생성한다")
	void create_shouldCreatePost() throws Exception {
		// given: 사용자/카테고리와 요청을 준비
		UserEntity user = persistUser("post-create@test.com");
		CategoriesEntity category = persistCategory("general");

		PostCreateRequest request = new PostCreateRequest();
		request.setCategoryId(category.getId());
		request.setTitle("제목");
		request.setContent("내용");

		// when: JWT subject로 게시글 생성 요청
		MvcResult result = mockMvc.perform(post("/posts")
				.with(jwt().jwt(jwt -> jwt.subject(user.getUuid().toString())))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isCreated())
			.andReturn();

		// then: 응답에 생성된 게시글 정보가 담긴다
		ApiResponse<PostResponse> apiResponse = objectMapper.readValue(
			result.getResponse().getContentAsString(),
			new TypeReference<ApiResponse<PostResponse>>() {}
		);

		assertThat(apiResponse.success()).isTrue();
		assertThat(apiResponse.data().userId()).isEqualTo(user.getId());
		assertThat(apiResponse.data().categoryId()).isEqualTo(category.getId());
		assertThat(apiResponse.data().title()).isEqualTo("제목");
		assertThat(apiResponse.data().content()).isEqualTo("내용");
	}

	@Test
	@DisplayName("JWT subject 기반으로 게시글을 수정한다")
	void update_shouldUpdatePost() throws Exception {
		// given: 사용자/카테고리와 기존 게시글을 준비
		UserEntity user = persistUser("post-update@test.com");
		CategoriesEntity category = persistCategory("update");
		PostsEntity post = persistPost(user, category, "기존 제목", "기존 내용");

		PostUpdateRequest request = new PostUpdateRequest();
		request.setTitle("수정 제목");
		request.setContent("수정 내용");

		// when: JWT subject로 게시글 수정 요청
		MvcResult result = mockMvc.perform(patch("/posts/{postId}", post.getId())
				.with(jwt().jwt(jwt -> jwt.subject(user.getUuid().toString())))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andReturn();

		// then: 응답에 수정된 게시글 정보가 담긴다
		ApiResponse<PostResponse> apiResponse = objectMapper.readValue(
			result.getResponse().getContentAsString(),
			new TypeReference<ApiResponse<PostResponse>>() {}
		);

		assertThat(apiResponse.success()).isTrue();
		assertThat(apiResponse.data().title()).isEqualTo("수정 제목");
		assertThat(apiResponse.data().content()).isEqualTo("수정 내용");
	}

	@Test
	@DisplayName("JWT subject 기반으로 게시글을 삭제한다")
	void delete_shouldMarkDeleted() throws Exception {
		// given: 사용자/카테고리와 기존 게시글을 준비
		UserEntity user = persistUser("post-delete@test.com");
		CategoriesEntity category = persistCategory("delete");
		PostsEntity post = persistPost(user, category, "삭제 제목", "삭제 내용");

		// when: JWT subject로 삭제 요청
		mockMvc.perform(delete("/posts/{postId}", post.getId())
				.with(jwt().jwt(jwt -> jwt.subject(user.getUuid().toString()))))
			.andExpect(status().isOk());

		// then: 소프트 삭제가 반영된다
		PostsEntity found = entityManager.find(PostsEntity.class, post.getId());
		assertThat(found.getDeletedAt()).isNotNull();
	}

	@Test
	@DisplayName("인증 토큰 없이 게시글 생성 요청 시 401을 반환한다")
	void create_withoutToken_shouldReturnUnauthorized() throws Exception {
		// given: 카테고리와 요청을 준비
		CategoriesEntity category = persistCategory("unauthorized");
		PostCreateRequest request = new PostCreateRequest();
		request.setCategoryId(category.getId());
		request.setTitle("제목");
		request.setContent("내용");

		// when & then: 토큰 없이 요청하면 401 응답
		mockMvc.perform(post("/posts")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("JWT subject가 비어 있으면 게시글 생성 요청 시 400을 반환한다")
	void create_withBlankSubject_shouldReturnBadRequest() throws Exception {
		// given: 카테고리와 요청을 준비
		CategoriesEntity category = persistCategory("blank-subject");
		PostCreateRequest request = new PostCreateRequest();
		request.setCategoryId(category.getId());
		request.setTitle("제목");
		request.setContent("내용");

		// when: 비어 있는 subject로 요청
		MvcResult result = mockMvc.perform(post("/posts")
				.with(jwt().jwt(jwt -> jwt.subject("")))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest())
			.andReturn();

		// then: 공통 400 에러 응답을 반환한다
		ApiResponse<Void> apiResponse = objectMapper.readValue(
			result.getResponse().getContentAsString(),
			new TypeReference<ApiResponse<Void>>() {}
		);
		assertThat(apiResponse.success()).isFalse();
		assertThat(apiResponse.error().code()).isEqualTo("COMMON_400");
	}

	@Test
	@DisplayName("JWT subject가 UUID 형식이 아니면 게시글 생성 요청 시 400을 반환한다")
	void create_withInvalidSubject_shouldReturnBadRequest() throws Exception {
		// given: 카테고리와 요청을 준비
		CategoriesEntity category = persistCategory("invalid-subject");
		PostCreateRequest request = new PostCreateRequest();
		request.setCategoryId(category.getId());
		request.setTitle("제목");
		request.setContent("내용");

		// when: 잘못된 UUID subject로 요청
		MvcResult result = mockMvc.perform(post("/posts")
				.with(jwt().jwt(jwt -> jwt.subject("not-a-uuid")))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest())
			.andReturn();

		// then: 공통 400 에러 응답을 반환한다
		ApiResponse<Void> apiResponse = objectMapper.readValue(
			result.getResponse().getContentAsString(),
			new TypeReference<ApiResponse<Void>>() {}
		);
		assertThat(apiResponse.success()).isFalse();
		assertThat(apiResponse.error().code()).isEqualTo("COMMON_400");
	}

	@Test
	@DisplayName("JWT subject가 UUID 형식이 아니면 게시글 수정 요청 시 400을 반환한다")
	void update_withInvalidSubject_shouldReturnBadRequest() throws Exception {
		// given: 사용자/카테고리와 기존 게시글을 준비
		UserEntity user = persistUser("post-update-invalid@test.com");
		CategoriesEntity category = persistCategory("update-invalid");
		PostsEntity post = persistPost(user, category, "기존 제목", "기존 내용");

		PostUpdateRequest request = new PostUpdateRequest();
		request.setTitle("수정 제목");
		request.setContent("수정 내용");

		// when: 잘못된 UUID subject로 수정 요청
		MvcResult result = mockMvc.perform(patch("/posts/{postId}", post.getId())
				.with(jwt().jwt(jwt -> jwt.subject("not-a-uuid")))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest())
			.andReturn();

		// then: 공통 400 에러 응답을 반환한다
		ApiResponse<Void> apiResponse = objectMapper.readValue(
			result.getResponse().getContentAsString(),
			new TypeReference<ApiResponse<Void>>() {}
		);
		assertThat(apiResponse.success()).isFalse();
		assertThat(apiResponse.error().code()).isEqualTo("COMMON_400");
	}

	@Test
	@DisplayName("JWT subject가 비어 있으면 게시글 수정 요청 시 400을 반환한다")
	void update_withBlankSubject_shouldReturnBadRequest() throws Exception {
		// given: 사용자/카테고리와 기존 게시글을 준비
		UserEntity user = persistUser("post-update-blank@test.com");
		CategoriesEntity category = persistCategory("update-blank");
		PostsEntity post = persistPost(user, category, "기존 제목", "기존 내용");

		PostUpdateRequest request = new PostUpdateRequest();
		request.setTitle("수정 제목");
		request.setContent("수정 내용");

		// when: 비어 있는 subject로 수정 요청
		MvcResult result = mockMvc.perform(patch("/posts/{postId}", post.getId())
				.with(jwt().jwt(jwt -> jwt.subject("")))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest())
			.andReturn();

		// then: 공통 400 에러 응답을 반환한다
		ApiResponse<Void> apiResponse = objectMapper.readValue(
			result.getResponse().getContentAsString(),
			new TypeReference<ApiResponse<Void>>() {}
		);
		assertThat(apiResponse.success()).isFalse();
		assertThat(apiResponse.error().code()).isEqualTo("COMMON_400");
	}

	@Test
	@DisplayName("JWT subject가 비어 있으면 게시글 삭제 요청 시 400을 반환한다")
	void delete_withBlankSubject_shouldReturnBadRequest() throws Exception {
		// given: 사용자/카테고리와 기존 게시글을 준비
		UserEntity user = persistUser("post-delete-blank@test.com");
		CategoriesEntity category = persistCategory("delete-blank");
		PostsEntity post = persistPost(user, category, "삭제 제목", "삭제 내용");

		// when: 비어 있는 subject로 삭제 요청
		MvcResult result = mockMvc.perform(delete("/posts/{postId}", post.getId())
				.with(jwt().jwt(jwt -> jwt.subject(""))))
			.andExpect(status().isBadRequest())
			.andReturn();

		// then: 공통 400 에러 응답을 반환한다
		ApiResponse<Void> apiResponse = objectMapper.readValue(
			result.getResponse().getContentAsString(),
			new TypeReference<ApiResponse<Void>>() {}
		);
		assertThat(apiResponse.success()).isFalse();
		assertThat(apiResponse.error().code()).isEqualTo("COMMON_400");
	}

	@Test
	@DisplayName("JWT subject가 UUID 형식이 아니면 게시글 삭제 요청 시 400을 반환한다")
	void delete_withInvalidSubject_shouldReturnBadRequest() throws Exception {
		// given: 사용자/카테고리와 기존 게시글을 준비
		UserEntity user = persistUser("post-delete-invalid@test.com");
		CategoriesEntity category = persistCategory("delete-invalid");
		PostsEntity post = persistPost(user, category, "삭제 제목", "삭제 내용");

		// when: 잘못된 UUID subject로 삭제 요청
		MvcResult result = mockMvc.perform(delete("/posts/{postId}", post.getId())
				.with(jwt().jwt(jwt -> jwt.subject("not-a-uuid"))))
			.andExpect(status().isBadRequest())
			.andReturn();

		// then: 공통 400 에러 응답을 반환한다
		ApiResponse<Void> apiResponse = objectMapper.readValue(
			result.getResponse().getContentAsString(),
			new TypeReference<ApiResponse<Void>>() {}
		);
		assertThat(apiResponse.success()).isFalse();
		assertThat(apiResponse.error().code()).isEqualTo("COMMON_400");
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
			PostStatus.PUBLISHED,
			user.getId()
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
