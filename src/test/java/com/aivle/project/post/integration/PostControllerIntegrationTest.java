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
import com.aivle.project.post.dto.PostResponse;
import com.aivle.project.post.dto.PostUserCreateRequest;
import com.aivle.project.post.dto.PostUserUpdateRequest;
import com.aivle.project.post.entity.PostStatus;
import com.aivle.project.post.entity.PostsEntity;
import com.aivle.project.user.entity.UserEntity;
import com.aivle.project.user.entity.UserStatus;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(TestSecurityConfig.class)
class PostControllerIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@PersistenceContext
	private EntityManager entityManager;

	private CategoriesEntity qnaCategory;
	private CategoriesEntity noticesCategory;

	@BeforeEach
	void setUp() {
		qnaCategory = findOrCreateCategory("qna");
		noticesCategory = findOrCreateCategory("notices");
	}

	private CategoriesEntity findOrCreateCategory(String name) {
		try {
			return entityManager.createQuery("select c from CategoriesEntity c where c.name = :name", CategoriesEntity.class)
				.setParameter("name", name)
				.getSingleResult();
		} catch (jakarta.persistence.NoResultException e) {
			CategoriesEntity category = newEntity(CategoriesEntity.class);
			ReflectionTestUtils.setField(category, "name", name);
			ReflectionTestUtils.setField(category, "isActive", true);
			entityManager.persist(category);
			entityManager.flush();
			return category;
		}
	}

	@Test
	@DisplayName("QnA 보드에 게시글을 생성한다")
	void create_shouldCreatePostInQna() throws Exception {
		// given
		UserEntity user = persistUser("qna-creator@test.com");
		PostUserCreateRequest request = new PostUserCreateRequest();
		request.setTitle("질문입니다");
		request.setContent("내용입니다");

		// when
		MvcResult result = null;
		try {
			result = mockMvc.perform(post("/api/posts/qna")
					.with(jwt().jwt(jwt -> jwt.subject(user.getUuid().toString()))
						.authorities(new SimpleGrantedAuthority("ROLE_USER")))
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andReturn();
			System.out.println("DEBUG STATUS: " + result.getResponse().getStatus());
			System.out.println("DEBUG RESPONSE: " + result.getResponse().getContentAsString());
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}

		assertThat(result.getResponse().getStatus()).isEqualTo(201);

		// then
		ApiResponse<PostResponse> apiResponse = objectMapper.readValue(
			result.getResponse().getContentAsString(),
			new TypeReference<ApiResponse<PostResponse>>() {}
		);

		assertThat(apiResponse.success()).isTrue();
		assertThat(apiResponse.data().title()).isEqualTo("질문입니다");
		assertThat(apiResponse.data().name()).isEqualTo("test-user");
		assertThat(apiResponse.data().categoryId()).isEqualTo(qnaCategory.getId());
	}

	@Test
	@DisplayName("Notices 보드에 일반 사용자가 글을 쓰면 403 Forbidden을 반환한다")
	void create_shouldFailInNotices() throws Exception {
		// given
		UserEntity user = persistUser("notice-hacker@test.com");
		PostUserCreateRequest request = new PostUserCreateRequest();
		request.setTitle("공지 해킹");
		request.setContent("내용");

		// when & then
		mockMvc.perform(post("/api/posts/notices")
				.with(jwt().jwt(jwt -> jwt.subject(user.getUuid().toString())))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("QnA 보드에서 본인의 글을 수정한다")
	void update_shouldUpdateOwnPost() throws Exception {
		// given
		UserEntity user = persistUser("qna-updater@test.com");
		PostsEntity post = persistPost(user, qnaCategory, "원본 제목", "원본 내용");

		PostUserUpdateRequest request = new PostUserUpdateRequest();
		request.setTitle("수정된 제목");
		request.setContent("수정된 내용");

		// when
		MvcResult result = mockMvc.perform(patch("/api/posts/qna/{postId}", post.getId())
				.with(jwt().jwt(jwt -> jwt.subject(user.getUuid().toString()))
					.authorities(new SimpleGrantedAuthority("ROLE_USER")))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andReturn();

		// then
		ApiResponse<PostResponse> apiResponse = objectMapper.readValue(
			result.getResponse().getContentAsString(),
			new TypeReference<ApiResponse<PostResponse>>() {}
		);
		assertThat(apiResponse.data().title()).isEqualTo("수정된 제목");
	}

	@Test
	@DisplayName("QnA 보드에서 타인의 글을 수정하면 403 Forbidden을 반환한다")
	void update_shouldFailForOtherPost() throws Exception {
		// given
		UserEntity owner = persistUser("owner@test.com");
		UserEntity hacker = persistUser("hacker@test.com");
		PostsEntity post = persistPost(owner, qnaCategory, "원본", "원본");

		PostUserUpdateRequest request = new PostUserUpdateRequest();
		request.setTitle("해킹");

		// when & then
		mockMvc.perform(patch("/api/posts/qna/{postId}", post.getId())
				.with(jwt().jwt(jwt -> jwt.subject(hacker.getUuid().toString()))
					.authorities(new SimpleGrantedAuthority("ROLE_USER")))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("Notices 보드의 글 목록은 누구나 조회 가능하다")
	void list_shouldReturnNotices() throws Exception {
		// given
		UserEntity admin = persistUser("admin@test.com"); // 작성자용 (실제론 어드민만 가능)
		for (int i = 0; i < 5; i++) {
			persistPost(admin, noticesCategory, "공지-" + i, "내용");
		}

		// when
		MvcResult result = mockMvc.perform(get("/api/posts/notices"))
			.andExpect(status().isOk())
			.andReturn();

		// then
		ApiResponse<PageResponse<PostResponse>> apiResponse = objectMapper.readValue(
			result.getResponse().getContentAsString(),
			new TypeReference<ApiResponse<PageResponse<PostResponse>>>() {}
		);
		assertThat(apiResponse.data().content()).hasSize(5);
	}

	@Test
	@DisplayName("QnA 보드에서 본인의 글 목록만 조회된다")
	void list_shouldReturnOnlyOwnQnaPosts() throws Exception {
		// given
		UserEntity userA = persistUser("userA@test.com");
		UserEntity userB = persistUser("userB@test.com");

		persistPost(userA, qnaCategory, "userA의 질문 1", "내용");
		persistPost(userA, qnaCategory, "userA의 질문 2", "내용");
		persistPost(userB, qnaCategory, "userB의 질문", "내용");

		// when
		MvcResult result = mockMvc.perform(get("/api/posts/qna")
				.with(jwt().jwt(jwt -> jwt.subject(userA.getUuid().toString()))
					.authorities(new SimpleGrantedAuthority("ROLE_USER"))))
			.andExpect(status().isOk())
			.andReturn();

		// then
		ApiResponse<PageResponse<PostResponse>> apiResponse = objectMapper.readValue(
			result.getResponse().getContentAsString(),
			new TypeReference<ApiResponse<PageResponse<PostResponse>>>() {}
		);

		assertThat(apiResponse.data().content()).hasSize(2);
		assertThat(apiResponse.data().content())
			.allMatch(post -> post.name().equals("test-user"));
		assertThat(apiResponse.data().content())
			.noneMatch(post -> post.title().contains("userB"));
	}

	@Test
	@DisplayName("QnA 보드에서 본인의 글을 삭제한다")
	void delete_shouldDeleteOwnPost() throws Exception {
		// given
		UserEntity user = persistUser("qna-deleter@test.com");
		PostsEntity post = persistPost(user, qnaCategory, "삭제용", "내용");

		// when
		mockMvc.perform(delete("/api/posts/qna/{postId}", post.getId())
				.with(jwt().jwt(jwt -> jwt.subject(user.getUuid().toString()))
					.authorities(new SimpleGrantedAuthority("ROLE_USER"))))
			.andExpect(status().isOk());

		// then
		PostsEntity found = entityManager.find(PostsEntity.class, post.getId());
		assertThat(found.getDeletedAt()).isNotNull();
	}

	@Test
	@DisplayName("REST 경로로 게시글을 생성한다")
	void create_shouldCreatePostWithRestPath() throws Exception {
		// given
		UserEntity user = persistUser("qna-creator-rest@test.com");
		String requestBody = """
			{
			  "categoryName": "qna",
			  "title": "REST 질문",
			  "content": "REST 내용"
			}
			""";

		// when
		MvcResult result = mockMvc.perform(post("/api/posts")
				.with(jwt().jwt(jwt -> jwt.subject(user.getUuid().toString()))
					.authorities(new SimpleGrantedAuthority("ROLE_USER")))
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andExpect(status().isCreated())
			.andReturn();

		// then
		ApiResponse<PostResponse> apiResponse = objectMapper.readValue(
			result.getResponse().getContentAsString(),
			new TypeReference<ApiResponse<PostResponse>>() {}
		);
		assertThat(apiResponse.success()).isTrue();
		assertThat(apiResponse.data().title()).isEqualTo("REST 질문");
	}

	@Test
	@DisplayName("REST 경로로 카테고리 기반 목록을 조회한다")
	void list_shouldReturnWithCategoryQueryParam() throws Exception {
		// given
		UserEntity admin = persistUser("rest-list@test.com");
		for (int i = 0; i < 3; i++) {
			persistPost(admin, noticesCategory, "REST-공지-" + i, "내용");
		}

		// when
		MvcResult result = mockMvc.perform(get("/api/posts")
				.param("categoryName", "notices"))
			.andExpect(status().isOk())
			.andReturn();

		// then
		ApiResponse<PageResponse<PostResponse>> apiResponse = objectMapper.readValue(
			result.getResponse().getContentAsString(),
			new TypeReference<ApiResponse<PageResponse<PostResponse>>>() {}
		);
		assertThat(apiResponse.success()).isTrue();
		assertThat(apiResponse.data().content()).hasSize(3);
	}

	@Test
	@DisplayName("REST 경로로 게시글을 수정한다")
	void update_shouldUpdateWithRestPath() throws Exception {
		// given
		UserEntity user = persistUser("rest-updater@test.com");
		PostsEntity post = persistPost(user, qnaCategory, "REST 원본", "REST 원본 내용");

		PostUserUpdateRequest request = new PostUserUpdateRequest();
		request.setTitle("REST 수정");

		// when
		MvcResult result = mockMvc.perform(patch("/api/posts/{postId}", post.getId())
				.with(jwt().jwt(jwt -> jwt.subject(user.getUuid().toString()))
					.authorities(new SimpleGrantedAuthority("ROLE_USER")))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andReturn();

		// then
		ApiResponse<PostResponse> apiResponse = objectMapper.readValue(
			result.getResponse().getContentAsString(),
			new TypeReference<ApiResponse<PostResponse>>() {}
		);
		assertThat(apiResponse.success()).isTrue();
		assertThat(apiResponse.data().title()).isEqualTo("REST 수정");
	}

	@Test
	@DisplayName("REST 경로로 게시글을 삭제한다")
	void delete_shouldDeleteWithRestPath() throws Exception {
		// given
		UserEntity user = persistUser("rest-deleter@test.com");
		PostsEntity post = persistPost(user, qnaCategory, "REST 삭제", "내용");

		// when
		mockMvc.perform(delete("/api/posts/{postId}", post.getId())
				.with(jwt().jwt(jwt -> jwt.subject(user.getUuid().toString()))
					.authorities(new SimpleGrantedAuthority("ROLE_USER"))))
			.andExpect(status().isOk());

		// then
		PostsEntity found = entityManager.find(PostsEntity.class, post.getId());
		assertThat(found.getDeletedAt()).isNotNull();
	}

	private UserEntity persistUser(String email) {
		UserEntity user = newEntity(UserEntity.class);
		ReflectionTestUtils.setField(user, "uuid", UUID.randomUUID());
		ReflectionTestUtils.setField(user, "email", email);
		ReflectionTestUtils.setField(user, "password", "encoded");
		ReflectionTestUtils.setField(user, "name", "test-user");
		ReflectionTestUtils.setField(user, "status", UserStatus.ACTIVE);
		entityManager.persist(user);
		return user;
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
			throw new IllegalStateException("엔티티 생성 실패", ex);
		}
	}
}
