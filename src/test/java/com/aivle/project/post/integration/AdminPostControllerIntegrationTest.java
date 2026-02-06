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
import com.aivle.project.post.dto.PostAdminCreateRequest;
import com.aivle.project.post.dto.PostAdminUpdateRequest;
import com.aivle.project.post.dto.PostResponse;
import com.aivle.project.post.entity.PostStatus;
import com.aivle.project.post.entity.PostsEntity;
import com.aivle.project.user.entity.UserEntity;
import com.aivle.project.user.entity.UserStatus;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
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
class AdminPostControllerIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@PersistenceContext
	private EntityManager entityManager;

	private CategoriesEntity qnaCategory;
	private CategoriesEntity noticesCategory;
	private UserEntity admin;

	@BeforeEach
	void setUp() {
		qnaCategory = findOrCreateCategory("qna");
		noticesCategory = findOrCreateCategory("notices");
		admin = persistUser("admin-controller@test.com");
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
	@DisplayName("관리자는 공지사항 보드에 글을 생성할 수 있다 (상단고정/상태 설정 포함)")
	void create_shouldCreateNotice() throws Exception {
		// given
		PostAdminCreateRequest request = new PostAdminCreateRequest();
		request.setTitle("중요 공지");
		request.setContent("필독");
		request.setPinned(true);
		request.setStatus(PostStatus.PUBLISHED);

		// when
		MvcResult result = mockMvc.perform(post("/api/admin/posts/notices")
				.with(jwt().jwt(jwt -> jwt.subject(admin.getUuid().toString()))
					.authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isCreated())
			.andReturn();

		// then
		ApiResponse<PostResponse> apiResponse = objectMapper.readValue(
			result.getResponse().getContentAsString(),
			new TypeReference<ApiResponse<PostResponse>>() {}
		);

		assertThat(apiResponse.success()).isTrue();
		assertThat(apiResponse.data().isPinned()).isTrue();
		assertThat(apiResponse.data().categoryId()).isEqualTo(noticesCategory.getId());
	}

	@Test
	@DisplayName("관리자는 타인의 QnA 게시글을 수정할 수 있다")
	void update_shouldUpdateOtherPost() throws Exception {
		// given
		UserEntity otherUser = persistUser("other@test.com");
		PostsEntity post = persistPost(otherUser, qnaCategory, "원본", "원본");

		PostAdminUpdateRequest request = new PostAdminUpdateRequest();
		request.setTitle("관리자 수정");
		request.setIsPinned(true);

		// when
		MvcResult result = mockMvc.perform(patch("/api/admin/posts/qna/{postId}", post.getId())
				.with(jwt().jwt(jwt -> jwt.subject(admin.getUuid().toString()))
					.authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andReturn();

		// then
		ApiResponse<PostResponse> apiResponse = objectMapper.readValue(
			result.getResponse().getContentAsString(),
			new TypeReference<ApiResponse<PostResponse>>() {}
		);
		assertThat(apiResponse.data().title()).isEqualTo("관리자 수정");
		assertThat(apiResponse.data().isPinned()).isTrue();
	}

	@Test
	@DisplayName("관리자는 타인의 게시글을 삭제할 수 있다")
	void delete_shouldDeletePost() throws Exception {
		// given
		UserEntity otherUser = persistUser("to-delete@test.com");
		PostsEntity post = persistPost(otherUser, qnaCategory, "삭제대상", "내용");

		// when
		mockMvc.perform(delete("/api/admin/posts/qna/{postId}", post.getId())
				.with(jwt().jwt(jwt -> jwt.subject(admin.getUuid().toString()))
					.authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
			.andExpect(status().isOk());

		// then
		PostsEntity found = entityManager.find(PostsEntity.class, post.getId());
		assertThat(found.getDeletedAt()).isNotNull();
	}

	@Test
	@DisplayName("일반 사용자가 관리자 API에 접근하면 403을 반환한다")
	void create_forbiddenForUser() throws Exception {
		// given
		UserEntity user = persistUser("user@test.com");
		PostAdminCreateRequest request = new PostAdminCreateRequest();
		request.setTitle("권한없음");
		request.setContent("내용");

		// when & then
		mockMvc.perform(post("/api/admin/posts/notices")
				.with(jwt().jwt(jwt -> jwt.subject(user.getUuid().toString()))
					.authorities(new SimpleGrantedAuthority("ROLE_USER")))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isForbidden());
	}

	private UserEntity persistUser(String email) {
		UserEntity user = newEntity(UserEntity.class);
		ReflectionTestUtils.setField(user, "uuid", UUID.randomUUID());
		ReflectionTestUtils.setField(user, "email", email);
		ReflectionTestUtils.setField(user, "password", "encoded");
		ReflectionTestUtils.setField(user, "name", "admin-tester");
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
