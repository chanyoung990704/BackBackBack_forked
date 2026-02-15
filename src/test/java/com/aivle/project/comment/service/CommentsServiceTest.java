package com.aivle.project.comment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.aivle.project.comment.dto.CommentCreateRequest;
import com.aivle.project.comment.dto.CommentResponse;
import com.aivle.project.comment.dto.CommentUpdateRequest;
import com.aivle.project.comment.entity.CommentsEntity;
import com.aivle.project.comment.repository.CommentsRepository;
import com.aivle.project.common.error.CommonException;
import com.aivle.project.post.entity.PostsEntity;
import com.aivle.project.post.repository.PostsRepository;
import com.aivle.project.post.service.PostReadAccessPolicy;
import com.aivle.project.user.entity.UserEntity;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CommentsServiceTest {

	@InjectMocks
	private CommentsService commentsService;

	@Mock
	private CommentsRepository commentsRepository;

	@Mock
	private PostsRepository postsRepository;

	@Mock
	private PostReadAccessPolicy postReadAccessPolicy;

	@Mock
	private com.aivle.project.comment.mapper.CommentMapper commentMapper;

	@Test
	@DisplayName("댓글 목록 조회 성공")
	void listByPost_success() {
		// given
		Long postId = 10L;
		CommentsEntity comment = mock(CommentsEntity.class);
		PostsEntity post = mock(PostsEntity.class);

		given(postsRepository.findByIdAndDeletedAtIsNull(postId)).willReturn(Optional.of(post));
		given(commentsRepository.findByPostIdAndDeletedAtIsNullOrderByDepthAscSequenceAsc(postId)).willReturn(List.of(comment));
		given(commentMapper.toResponse(comment)).willReturn(new CommentResponse(1L, "홍길동", postId, null, "댓글", 0, 0, null, null));

		// when
		List<CommentResponse> result = commentsService.listByPost(postId, null);

		// then
		assertThat(result).hasSize(1);
	}

	@Test
	@DisplayName("댓글 생성 성공 - 최상위 댓글")
	void create_topLevel_success() {
		// given
		Long userId = 1L;
		Long postId = 10L;

		CommentCreateRequest request = new CommentCreateRequest();
		request.setPostId(postId);
		request.setContent("첫 번째 댓글");

		UserEntity user = mock(UserEntity.class);
		given(user.getId()).willReturn(userId);

		PostsEntity post = mock(PostsEntity.class);
		given(post.getId()).willReturn(postId);

		given(postsRepository.findByIdAndDeletedAtIsNull(postId)).willReturn(Optional.of(post));
		given(commentsRepository.findMaxSequenceByPostIdAndParentIsNull(postId)).willReturn(-1);
		given(commentsRepository.save(any(CommentsEntity.class))).willAnswer(invocation -> {
			CommentsEntity saved = invocation.getArgument(0);
			ReflectionTestUtils.setField(saved, "id", 100L);
			return saved;
		});

		given(commentMapper.toResponse(any(CommentsEntity.class))).willAnswer(invocation -> {
			CommentsEntity c = invocation.getArgument(0);
			return new CommentResponse(c.getId(), "홍길동", postId, null, c.getContent(), c.getDepth(), c.getSequence(), null, null);
		});

		// when
		CommentResponse response = commentsService.create(user, request);

		// then
		assertThat(response.id()).isEqualTo(100L);
		assertThat(response.name()).isEqualTo("홍길동");
		assertThat(response.content()).isEqualTo("첫 번째 댓글");
		assertThat(response.depth()).isZero();
		assertThat(response.sequence()).isZero(); // -1 + 1 = 0
		assertThat(response.parentId()).isNull();
	}

	@Test
	@DisplayName("댓글 생성 성공 - 대댓글")
	void create_reply_success() {
		// given
		Long userId = 1L;
		Long postId = 10L;
		Long parentId = 100L;

		CommentCreateRequest request = new CommentCreateRequest();
		request.setPostId(postId);
		request.setParentId(parentId);
		request.setContent("대댓글");

		UserEntity user = mock(UserEntity.class);
		given(user.getId()).willReturn(userId);

		PostsEntity post = mock(PostsEntity.class);
		given(post.getId()).willReturn(postId);

		CommentsEntity parent = mock(CommentsEntity.class);
		given(parent.getId()).willReturn(parentId);
		given(parent.getPost()).willReturn(post); // 부모 댓글은 같은 게시글이어야 함
		given(parent.getDepth()).willReturn(0);

		given(postsRepository.findByIdAndDeletedAtIsNull(postId)).willReturn(Optional.of(post));
		given(commentsRepository.findById(parentId)).willReturn(Optional.of(parent));
		given(commentsRepository.findMaxSequenceByParentId(parentId)).willReturn(0); // 기존 자식 1개 있음 가정
		given(commentsRepository.save(any(CommentsEntity.class))).willAnswer(invocation -> {
			CommentsEntity saved = invocation.getArgument(0);
			ReflectionTestUtils.setField(saved, "id", 101L);
			return saved;
		});

		given(commentMapper.toResponse(any(CommentsEntity.class))).willAnswer(invocation -> {
			CommentsEntity c = invocation.getArgument(0);
			return new CommentResponse(c.getId(), "홍길동", postId, parentId, c.getContent(), c.getDepth(), c.getSequence(), null, null);
		});

		// when
		CommentResponse response = commentsService.create(user, request);

		// then
		assertThat(response.id()).isEqualTo(101L);
		assertThat(response.name()).isEqualTo("홍길동");
		assertThat(response.depth()).isEqualTo(1); // parent depth(0) + 1
		assertThat(response.sequence()).isEqualTo(1); // max(0) + 1
		assertThat(response.parentId()).isEqualTo(parentId);
	}

	@Test
	@DisplayName("댓글 생성 실패 - 게시글이 존재하지 않음")
	void create_fail_postNotFound() {
		// given
		Long userId = 1L;
		CommentCreateRequest request = new CommentCreateRequest();
		request.setPostId(999L);
		request.setContent("댓글");

		UserEntity user = mock(UserEntity.class);
		given(user.getId()).willReturn(userId);
		given(postsRepository.findByIdAndDeletedAtIsNull(999L)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> commentsService.create(user, request))
			.isInstanceOf(CommonException.class);
	}

	@Test
	@DisplayName("댓글 수정 성공")
	void update_success() {
		// given
		Long userId = 1L;
		Long commentId = 100L;

		CommentUpdateRequest request = new CommentUpdateRequest();
		request.setContent("수정된 내용");

		UserEntity user = mock(UserEntity.class);
		given(user.getId()).willReturn(userId);

		CommentsEntity realComment = CommentsEntity.create(mock(PostsEntity.class), user, null, "원래 내용", 0, 0);
		ReflectionTestUtils.setField(realComment, "id", commentId);

		given(commentsRepository.findById(commentId)).willReturn(Optional.of(realComment));
		given(commentMapper.toResponse(any(CommentsEntity.class))).willAnswer(invocation -> {
			CommentsEntity c = invocation.getArgument(0);
			return new CommentResponse(c.getId(), "홍길동", null, null, c.getContent(), c.getDepth(), c.getSequence(), null, null);
		});

		// when
		CommentResponse response = commentsService.update(userId, commentId, request);

		// then
		assertThat(response.content()).isEqualTo("수정된 내용");
		assertThat(response.name()).isEqualTo("홍길동");
	}

	@Test
	@DisplayName("댓글 수정 실패 - 작성자가 아님")
	void update_fail_notOwner() {
		// given
		Long userId = 1L;
		Long otherUserId = 2L;
		Long commentId = 100L;

		CommentUpdateRequest request = new CommentUpdateRequest();
		request.setContent("수정 시도");

		UserEntity owner = mock(UserEntity.class);
		given(owner.getId()).willReturn(otherUserId);

		CommentsEntity comment = mock(CommentsEntity.class);
		given(comment.getUser()).willReturn(owner);

		given(commentsRepository.findById(commentId)).willReturn(Optional.of(comment));

		// when & then
		assertThatThrownBy(() -> commentsService.update(userId, commentId, request))
			.isInstanceOf(CommonException.class);
	}

	@Test
	@DisplayName("댓글 삭제 성공")
	void delete_success() {
		// given
		Long userId = 1L;
		Long commentId = 100L;

		UserEntity user = mock(UserEntity.class);
		given(user.getId()).willReturn(userId);

		CommentsEntity comment = CommentsEntity.create(mock(PostsEntity.class), user, null, "내용", 0, 0);
		ReflectionTestUtils.setField(comment, "id", commentId);

		given(commentsRepository.findById(commentId)).willReturn(Optional.of(comment));

		// when
		commentsService.delete(userId, commentId);

		// then
		assertThat(comment.isDeleted()).isTrue();
	}
}
