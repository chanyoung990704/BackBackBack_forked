package com.aivle.project.comment.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aivle.project.category.entity.CategoriesEntity;
import com.aivle.project.comment.dto.CommentCreateRequest;
import com.aivle.project.comment.dto.CommentResponse;
import com.aivle.project.comment.dto.CommentUpdateRequest;
import com.aivle.project.comment.service.CommentsService;
import com.aivle.project.common.config.TestSecurityConfig;
import com.aivle.project.common.dto.ApiResponse;
import com.aivle.project.post.entity.PostStatus;
import com.aivle.project.post.entity.PostsEntity;
import com.aivle.project.user.entity.UserEntity;
import com.aivle.project.user.entity.UserStatus;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
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
@ActiveProfiles("test")
@Transactional
@Import(TestSecurityConfig.class)
class CommentControllerIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private CommentsService commentsService;

	@PersistenceContext
	private EntityManager entityManager;

	@Test
	@DisplayName("댓글을 생성한다")
	void create_shouldCreateComment() throws Exception {
		// given
		UserEntity user = persistUser("comment-create@test.com");
		CategoriesEntity category = persistCategory("comment");
		PostsEntity post = persistPost(user, category, "title", "content");

		CommentCreateRequest request = new CommentCreateRequest();
		request.setPostId(post.getId());
		request.setContent("댓글 내용");

		// when
		MvcResult result = mockMvc.perform(post("/api/posts/{postId}/comments", post.getId())
				.with(jwt().jwt(jwt -> jwt.subject(user.getUuid().toString())))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isCreated())
			.andReturn();

		// then
		ApiResponse<CommentResponse> apiResponse = objectMapper.readValue(
			result.getResponse().getContentAsString(),
			new TypeReference<ApiResponse<CommentResponse>>() {}
		);
		assertThat(apiResponse.success()).isTrue();
		assertThat(apiResponse.data().content()).isEqualTo("댓글 내용");
		assertThat(apiResponse.data().postId()).isEqualTo(post.getId());
	}

	@Test
	@DisplayName("게시글 댓글 목록을 조회한다")
	void list_shouldReturnComments() throws Exception {
		// given
		UserEntity user = persistUser("comment-list@test.com");
		CategoriesEntity category = persistCategory("comment");
		PostsEntity post = persistPost(user, category, "title", "content");

		CommentCreateRequest request = new CommentCreateRequest();
		request.setPostId(post.getId());
		request.setContent("댓글 1");
		commentsService.create(user, request);

		request.setContent("댓글 2");
		commentsService.create(user, request);

		// when
		MvcResult result = mockMvc.perform(get("/api/posts/{postId}/comments", post.getId())
				.with(jwt().jwt(jwt -> jwt.subject(user.getUuid().toString()))))
			.andExpect(status().isOk())
			.andReturn();

		// then
		ApiResponse<List<CommentResponse>> apiResponse = objectMapper.readValue(
			result.getResponse().getContentAsString(),
			new TypeReference<ApiResponse<List<CommentResponse>>>() {}
		);
			assertThat(apiResponse.data()).hasSize(2);
	}

	@Test
	@DisplayName("QnA 댓글 목록은 비로그인 사용자가 조회할 수 없다")
	void list_qna_shouldFailWhenAnonymous() throws Exception {
		// given
		UserEntity owner = persistUser("comment-qna-owner@test.com");
		CategoriesEntity qna = persistCategory("qna");
		PostsEntity post = persistPost(owner, qna, "qna", "content");

		CommentCreateRequest request = new CommentCreateRequest();
		request.setPostId(post.getId());
		request.setContent("비밀 댓글");
		commentsService.create(owner, request);

		// when & then
		mockMvc.perform(get("/api/posts/{postId}/comments", post.getId()))
			.andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("QnA 댓글 목록은 작성자가 아니면 조회할 수 없다")
	void list_qna_shouldFailWhenNotOwner() throws Exception {
		// given
		UserEntity owner = persistUser("comment-qna-owner2@test.com");
		UserEntity other = persistUser("comment-qna-other@test.com");
		CategoriesEntity qna = persistCategory("qna");
		PostsEntity post = persistPost(owner, qna, "qna", "content");

		CommentCreateRequest request = new CommentCreateRequest();
		request.setPostId(post.getId());
		request.setContent("비밀 댓글");
		commentsService.create(owner, request);

		// when & then
		mockMvc.perform(get("/api/posts/{postId}/comments", post.getId())
				.with(jwt().jwt(jwt -> jwt.subject(other.getUuid().toString()))))
			.andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("댓글을 수정한다")
	void update_shouldUpdateComment() throws Exception {
		// given
		UserEntity user = persistUser("comment-update@test.com");
		CategoriesEntity category = persistCategory("comment");
		PostsEntity post = persistPost(user, category, "title", "content");

		CommentCreateRequest createRequest = new CommentCreateRequest();
		createRequest.setPostId(post.getId());
		createRequest.setContent("댓글 내용");
		CommentResponse created = commentsService.create(user, createRequest);

		CommentUpdateRequest updateRequest = new CommentUpdateRequest();
		updateRequest.setContent(" 수정 ");

		// when
		MvcResult result = mockMvc.perform(patch("/api/comments/{commentId}", created.id())
				.with(jwt().jwt(jwt -> jwt.subject(user.getUuid().toString())))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(updateRequest)))
			.andExpect(status().isOk())
			.andReturn();

		// then
		ApiResponse<CommentResponse> apiResponse = objectMapper.readValue(
			result.getResponse().getContentAsString(),
			new TypeReference<ApiResponse<CommentResponse>>() {}
		);
		assertThat(apiResponse.data().content()).isEqualTo("수정");
	}

	@Test
	@DisplayName("댓글을 삭제한다")
	void delete_shouldSoftDeleteComment() throws Exception {
		// given
		UserEntity user = persistUser("comment-delete@test.com");
		CategoriesEntity category = persistCategory("comment");
		PostsEntity post = persistPost(user, category, "title", "content");

		CommentCreateRequest createRequest = new CommentCreateRequest();
		createRequest.setPostId(post.getId());
		createRequest.setContent("댓글 내용");
		CommentResponse created = commentsService.create(user, createRequest);

		// when
		mockMvc.perform(delete("/api/comments/{commentId}", created.id())
				.with(jwt().jwt(jwt -> jwt.subject(user.getUuid().toString()))))
			.andExpect(status().isOk());

		// then
		var comment = entityManager.find(com.aivle.project.comment.entity.CommentsEntity.class, created.id());
		assertThat(comment.getDeletedAt()).isNotNull();
	}

	private UserEntity persistUser(String email) {
		UserEntity user = newEntity(UserEntity.class);
		ReflectionTestUtils.setField(user, "email", email);
		ReflectionTestUtils.setField(user, "password", "encoded");
		ReflectionTestUtils.setField(user, "name", "user");
		ReflectionTestUtils.setField(user, "status", UserStatus.ACTIVE);
		entityManager.persist(user);
		entityManager.flush();
		return user;
	}

	private CategoriesEntity persistCategory(String name) {
		CategoriesEntity existing = entityManager.createQuery(
			"select c from CategoriesEntity c where c.name = :name",
			CategoriesEntity.class
		)
			.setParameter("name", name)
			.getResultStream()
			.findFirst()
			.orElse(null);
		if (existing != null) {
			return existing;
		}

		CategoriesEntity category = newEntity(CategoriesEntity.class);
		ReflectionTestUtils.setField(category, "name", name);
		entityManager.persist(category);
		entityManager.flush();
		return category;
	}

	private PostsEntity persistPost(UserEntity user, CategoriesEntity category, String title, String content) {
		PostsEntity post = PostsEntity.create(user, category, title, content, false, PostStatus.PUBLISHED);
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
