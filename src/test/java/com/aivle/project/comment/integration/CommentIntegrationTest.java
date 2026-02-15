package com.aivle.project.comment.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.aivle.project.category.entity.CategoriesEntity;
import com.aivle.project.comment.dto.CommentCreateRequest;
import com.aivle.project.comment.dto.CommentResponse;
import com.aivle.project.comment.service.CommentsService;
import com.aivle.project.common.config.TestSecurityConfig;
import com.aivle.project.common.util.NameMaskingUtil;
import com.aivle.project.post.entity.PostStatus;
import com.aivle.project.post.entity.PostsEntity;
import com.aivle.project.user.entity.UserEntity;
import com.aivle.project.user.entity.UserStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import(TestSecurityConfig.class)
class CommentIntegrationTest {

	@Autowired
	private CommentsService commentsService;

	@PersistenceContext
	private EntityManager entityManager;

	@Test
	@DisplayName("사용자-게시글-댓글-대댓글 흐름이 정상 동작한다")
	void create_fullCommentFlow() {
		// given: 사용자/카테고리/게시글을 준비
		UserEntity writer = persistUser("writer@test.com", "작성자");
		UserEntity commenter = persistUser("commenter@test.com", "댓글러");
		CategoriesEntity category = persistCategory("자유게시판-통합-테스트");
		PostsEntity post = persistPost(writer, category, "통합 테스트 제목", "내용입니다");

		// when: 댓글과 대댓글을 작성
		CommentCreateRequest parentRequest = new CommentCreateRequest();
		parentRequest.setPostId(post.getId());
		parentRequest.setContent("댓글 내용");
		CommentResponse parentResponse = commentsService.create(commenter, parentRequest);

		CommentCreateRequest childRequest = new CommentCreateRequest();
		childRequest.setPostId(post.getId());
		childRequest.setParentId(parentResponse.id());
		childRequest.setContent("대댓글 내용");
		CommentResponse childResponse = commentsService.create(writer, childRequest);

		// then: 계층 구조가 올바르게 조회된다
		List<CommentResponse> responses = commentsService.listByPost(post.getId(), null);
		assertThat(responses).hasSize(2);

		CommentResponse root = responses.get(0);
		assertThat(root.id()).isEqualTo(parentResponse.id());
		assertThat(root.name()).isEqualTo(NameMaskingUtil.mask("댓글러"));
		assertThat(root.depth()).isZero();

		CommentResponse reply = responses.get(1);
		assertThat(reply.id()).isEqualTo(childResponse.id());
		assertThat(reply.parentId()).isEqualTo(parentResponse.id());
		assertThat(reply.name()).isEqualTo(NameMaskingUtil.mask("작성자"));
		assertThat(reply.depth()).isEqualTo(1);
	}

	@Test
	@DisplayName("여러 대댓글이 순서대로 생성되고 조회된다")
	void list_multipleReplies_shouldOrderBySequence() {
		// given: 사용자/카테고리/게시글을 준비
		UserEntity writer = persistUser("writer-multi@test.com", "작성자");
		UserEntity commenter = persistUser("commenter-multi@test.com", "댓글러");
		CategoriesEntity category = persistCategory("자유게시판-다중-테스트");
		PostsEntity post = persistPost(writer, category, "다중 대댓글", "내용입니다");

		CommentCreateRequest parentRequest = new CommentCreateRequest();
		parentRequest.setPostId(post.getId());
		parentRequest.setContent("부모 댓글");
		CommentResponse parent = commentsService.create(commenter, parentRequest);

		CommentCreateRequest replyOneRequest = new CommentCreateRequest();
		replyOneRequest.setPostId(post.getId());
		replyOneRequest.setParentId(parent.id());
		replyOneRequest.setContent("대댓글 1");
		CommentResponse replyOne = commentsService.create(writer, replyOneRequest);

		CommentCreateRequest replyTwoRequest = new CommentCreateRequest();
		replyTwoRequest.setPostId(post.getId());
		replyTwoRequest.setParentId(parent.id());
		replyTwoRequest.setContent("대댓글 2");
		CommentResponse replyTwo = commentsService.create(writer, replyTwoRequest);

		// when
		List<CommentResponse> responses = commentsService.listByPost(post.getId(), null);

		// then
		assertThat(responses).hasSize(3);
		assertThat(responses.get(1).id()).isEqualTo(replyOne.id());
		assertThat(responses.get(2).id()).isEqualTo(replyTwo.id());
		assertThat(responses.get(1).sequence()).isEqualTo(0);
		assertThat(responses.get(2).sequence()).isEqualTo(1);
	}

	private UserEntity persistUser(String email, String name) {
		UserEntity user = UserEntity.create(email, "encoded-password", name, "010-0000-0000", UserStatus.ACTIVE);
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
