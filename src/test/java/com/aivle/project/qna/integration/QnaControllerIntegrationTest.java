package com.aivle.project.qna.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aivle.project.category.entity.CategoriesEntity;
import com.aivle.project.common.config.TestSecurityConfig;
import com.aivle.project.common.dto.ApiResponse;
import com.aivle.project.qna.dto.QaPostCreateRequest;
import com.aivle.project.qna.dto.QaPostResponse;
import com.aivle.project.qna.dto.QaReplyCreateRequest;
import com.aivle.project.qna.service.QnaService;
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

import java.util.List;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(TestSecurityConfig.class)
class QnaControllerIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private QnaService qnaService;

	@PersistenceContext
	private EntityManager entityManager;

	@Test
	@DisplayName("사용자가 자신의 Q&A 목록을 조회한다")
	void listMyQna_shouldReturnUserPosts() throws Exception {
		// given
		UserEntity user = persistUser("user1@test.com", "사용자");
		persistCategory("QNA");
		qnaService.create(user, new QaPostCreateRequest("내 질문", "내용", "사용자"));
		entityManager.flush();

		// when
		MvcResult result = mockMvc.perform(get("/api/qna")
				.with(jwt().jwt(jwt -> jwt.issuer("project-local")
					.subject(user.getUuid().toString())
					.claim("userId", user.getId())
					.claim("roles", List.of("ROLE_USER")))))
			.andExpect(status().isOk())
			.andReturn();

		// then
		ApiResponse<List<QaPostResponse>> apiResponse = objectMapper.readValue(
			result.getResponse().getContentAsString(),
			new TypeReference<ApiResponse<List<QaPostResponse>>>() {}
		);
		assertThat(apiResponse.data()).hasSize(1);
		assertThat(apiResponse.data().get(0).title()).isEqualTo("내 질문");
	}

	@Test
	@DisplayName("사용자가 새로운 Q&A 질문을 생성한다")
	void create_shouldCreateQuestion() throws Exception {
		// given
		UserEntity user = persistUser("user-create@test.com", "질문자");
		persistCategory("QNA");
		QaPostCreateRequest request = new QaPostCreateRequest("질문 제목", "질문 내용", "질문자");

		// when
		MvcResult result = mockMvc.perform(post("/api/qna")
				.with(jwt().jwt(jwt -> jwt.issuer("project-local")
					.subject(user.getUuid().toString())
					.claim("userId", user.getId())
					.claim("roles", List.of("ROLE_USER"))))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isCreated())
			.andReturn();

		// then
		ApiResponse<QaPostResponse> apiResponse = objectMapper.readValue(
			result.getResponse().getContentAsString(),
			new TypeReference<ApiResponse<QaPostResponse>>() {}
		);
		assertThat(apiResponse.data().title()).isEqualTo("질문 제목");
	}

	@Test
	@DisplayName("관리자가 전체 Q&A 목록을 조회한다")
	void listAll_shouldReturnAllPostsForAdmin() throws Exception {
		// given
		UserEntity admin = persistUser("admin1@test.com", "관리자");
		UserEntity user = persistUser("user2@test.com", "사용자");
		persistCategory("QNA");
		qnaService.create(user, new QaPostCreateRequest("사용자 질문", "내용", "사용자"));
		entityManager.flush();

		// when
		MvcResult result = mockMvc.perform(get("/api/admin/qna")
				.with(jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"))
					.jwt(jwt -> jwt.issuer("project-local")
						.subject(admin.getUuid().toString())
						.claim("userId", admin.getId()))))
			.andExpect(status().isOk())
			.andReturn();

		// then
		ApiResponse<List<QaPostResponse>> apiResponse = objectMapper.readValue(
			result.getResponse().getContentAsString(),
			new TypeReference<ApiResponse<List<QaPostResponse>>>() {}
		);
		assertThat(apiResponse.data()).isNotEmpty();
		assertThat(apiResponse.data().stream().anyMatch(p -> p.title().equals("사용자 질문"))).isTrue();
	}

	@Test
	@DisplayName("관리자가 Q&A에 답변을 추가하면 상태가 answered로 변경된다")
	void addReply_shouldChangeStatusToAnswered() throws Exception {
		// given
		UserEntity admin = persistUser("admin2@test.com", "데스크");
		UserEntity user = persistUser("user3@test.com", "사용자");
		persistCategory("QNA");
		QaPostResponse postResponse = qnaService.create(user, new QaPostCreateRequest("질문", "내용", "사용자"));
		Long postId = Long.valueOf(postResponse.id());
		entityManager.flush();

		QaReplyCreateRequest request = new QaReplyCreateRequest("답변입니다.", "데스크");

		// when: 관리자가 답변 추가
		mockMvc.perform(post("/api/admin/qna/{id}/replies", postId)
				.with(jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"))
					.jwt(jwt -> jwt.issuer("project-local")
						.subject(admin.getUuid().toString())
						.claim("userId", admin.getId())))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isCreated());

		entityManager.flush();

		// then: 사용자가 목록 조회 시 status가 answered여야 함
		MvcResult result = mockMvc.perform(get("/api/qna")
				.with(jwt().jwt(jwt -> jwt.issuer("project-local")
					.subject(user.getUuid().toString())
					.claim("userId", user.getId())
					.claim("roles", List.of("ROLE_USER")))))
			.andExpect(status().isOk())
			.andReturn();

		ApiResponse<List<QaPostResponse>> apiResponse = objectMapper.readValue(
			result.getResponse().getContentAsString(),
			new TypeReference<ApiResponse<List<QaPostResponse>>>() {}
		);
		assertThat(apiResponse.data()).isNotEmpty();
		assertThat(apiResponse.data().get(0).status()).isEqualTo("answered");
	}

	private UserEntity persistUser(String email, String name) {
		UserEntity user = UserEntity.create(email, "encoded", name, "010-0000-0000", UserStatus.ACTIVE);
		ReflectionTestUtils.setField(user, "uuid", UUID.randomUUID());
		entityManager.persist(user);
		return user;
	}

	private CategoriesEntity persistCategory(String name) {
		CategoriesEntity category = CategoriesEntity.create(name, name, 0, true);
		entityManager.persist(category);
		entityManager.flush();
		return category;
	}
}
