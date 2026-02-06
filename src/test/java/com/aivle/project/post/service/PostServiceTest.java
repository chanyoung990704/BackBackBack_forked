package com.aivle.project.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.aivle.project.category.entity.CategoriesEntity;
import com.aivle.project.category.repository.CategoriesRepository;
import com.aivle.project.common.dto.PageRequest;
import com.aivle.project.common.dto.PageResponse;
import com.aivle.project.common.error.CommonErrorCode;
import com.aivle.project.common.error.CommonException;
import com.aivle.project.post.dto.PostAdminCreateRequest;
import com.aivle.project.post.dto.PostAdminUpdateRequest;
import com.aivle.project.post.dto.PostResponse;
import com.aivle.project.post.dto.PostUserCreateRequest;
import com.aivle.project.post.dto.PostUserUpdateRequest;
import com.aivle.project.post.entity.PostStatus;
import com.aivle.project.post.entity.PostsEntity;
import com.aivle.project.post.entity.PostViewCountsEntity;
import com.aivle.project.post.repository.PostViewCountsRepository;
import com.aivle.project.post.repository.PostsRepository;
import com.aivle.project.user.entity.UserEntity;
import com.aivle.project.user.entity.UserStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

	@InjectMocks
	private PostService postService;

	@Mock
	private PostsRepository postsRepository;

	@Mock
	private PostViewCountsRepository postViewCountsRepository;

	@Mock
	private CategoriesRepository categoriesRepository;

	@Mock
	private com.aivle.project.post.mapper.PostMapper postMapper;

	// User Operations Tests

	@Test
	@DisplayName("사용자는 'notices' 보드에 글을 작성할 수 없다 (403 Forbidden)")
	void create_shouldFailForNotices() {
		// given
		UserEntity user = newUser(1L);
		PostUserCreateRequest request = new PostUserCreateRequest();
		request.setTitle("title");
		request.setContent("content");

		// when & then
		assertThatThrownBy(() -> postService.create("notices", user, request))
			.isInstanceOf(CommonException.class)
			.extracting(ex -> ((CommonException) ex).getErrorCode())
			.isEqualTo(CommonErrorCode.COMMON_403);
	}

	@Test
	@DisplayName("사용자는 'qna' 보드에 글을 작성할 수 있다")
	void create_shouldSucceedForQna() {
		// given
		UserEntity user = newUser(1L);
		CategoriesEntity category = newCategory(2L, "qna");
		PostUserCreateRequest request = new PostUserCreateRequest();
		request.setTitle("qna title");
		request.setContent("qna content");

		given(categoriesRepository.findByNameAndDeletedAtIsNull("qna")).willReturn(Optional.of(category));
		given(postsRepository.save(any(PostsEntity.class))).willAnswer(invocation -> {
			PostsEntity p = invocation.getArgument(0);
			ReflectionTestUtils.setField(p, "id", 100L);
			return p;
		});
		given(postMapper.toResponse(any(PostsEntity.class))).willReturn(new PostResponse(100L, 1L, 2L, "qna title", "qna content", 0, false, PostStatus.PUBLISHED, null, null));

		// when
		PostResponse response = postService.create("qna", user, request);

		// then
		assertThat(response.id()).isEqualTo(100L);
		verify(postViewCountsRepository).save(any(PostViewCountsEntity.class));
	}

	@Test
	@DisplayName("사용자는 본인의 글을 수정할 수 있다")
	void update_shouldSucceedForOwner() {
		// given
		UserEntity user = newUser(1L);
		PostsEntity post = newPost(100L, user, newCategory(2L, "qna"));
		PostUserUpdateRequest request = new PostUserUpdateRequest();
		request.setTitle("updated");

		given(postsRepository.findByIdAndCategoryNameAndDeletedAtIsNull(100L, "qna"))
			.willReturn(Optional.of(post));
		given(postMapper.toResponse(post)).willReturn(new PostResponse(100L, 1L, 2L, "updated", "content", 0, false, PostStatus.PUBLISHED, null, null));

		// when
		PostResponse response = postService.update("qna", user, 100L, request);

		// then
		assertThat(response.title()).isEqualTo("updated");
	}

	@Test
	@DisplayName("사용자는 타인의 글을 수정할 수 없다 (403 Forbidden)")
	void update_shouldFailForNonOwner() {
		// given
		UserEntity owner = newUser(1L);
		UserEntity other = newUser(2L);
		PostsEntity post = newPost(100L, owner, newCategory(2L, "qna"));
		PostUserUpdateRequest request = new PostUserUpdateRequest();

		given(postsRepository.findByIdAndCategoryNameAndDeletedAtIsNull(100L, "qna"))
			.willReturn(Optional.of(post));

		// when & then
		assertThatThrownBy(() -> postService.update("qna", other, 100L, request))
			.isInstanceOf(CommonException.class);
	}

	// Admin Operations Tests

	@Test
	@DisplayName("관리자는 'notices' 보드에 글을 작성할 수 있다")
	void createAdmin_shouldSucceedForNotices() {
		// given
		UserEntity admin = newUser(99L);
		CategoriesEntity category = newCategory(1L, "notices");
		PostAdminCreateRequest request = new PostAdminCreateRequest();
		request.setTitle("notice");
		request.setContent("content");
		request.setPinned(true);
		request.setStatus(PostStatus.PUBLISHED);

		given(categoriesRepository.findByNameAndDeletedAtIsNull("notices")).willReturn(Optional.of(category));
		given(postsRepository.save(any(PostsEntity.class))).willAnswer(invocation -> {
			PostsEntity p = invocation.getArgument(0);
			ReflectionTestUtils.setField(p, "id", 200L);
			return p;
		});
		given(postMapper.toResponse(any(PostsEntity.class))).willReturn(new PostResponse(200L, 99L, 1L, "notice", "content", 0, true, PostStatus.PUBLISHED, null, null));

		// when
		PostResponse response = postService.createAdmin("notices", admin, request);

		// then
		assertThat(response.id()).isEqualTo(200L);
		assertThat(response.isPinned()).isTrue();
	}

	@Test
	@DisplayName("관리자는 타인의 글도 수정할 수 있다")
	void updateAdmin_shouldSucceedForAnyPost() {
		// given
		UserEntity user = newUser(1L);
		PostsEntity post = newPost(100L, user, newCategory(2L, "qna"));
		PostAdminUpdateRequest request = new PostAdminUpdateRequest();
		request.setTitle("admin updated");
		request.setIsPinned(true);

		given(postsRepository.findByIdAndCategoryNameAndDeletedAtIsNull(100L, "qna"))
			.willReturn(Optional.of(post));
		given(postMapper.toResponse(post)).willReturn(new PostResponse(100L, 1L, 2L, "admin updated", "content", 0, true, PostStatus.PUBLISHED, null, null));

		// when
		PostResponse response = postService.updateAdmin("qna", 100L, request);

		// then
		assertThat(response.title()).isEqualTo("admin updated");
		assertThat(response.isPinned()).isTrue();
	}

	@Test
	@DisplayName("관리자는 타인의 글을 삭제할 수 있다")
	void deleteAdmin_shouldSucceed() {
		// given
		UserEntity user = newUser(1L);
		PostsEntity post = newPost(100L, user, newCategory(2L, "qna"));

		given(postsRepository.findByIdAndCategoryNameAndDeletedAtIsNull(100L, "qna"))
			.willReturn(Optional.of(post));

		// when
		postService.deleteAdmin("qna", 100L);

		// then
		assertThat(post.isDeleted()).isTrue();
	}

	// Helpers

	private UserEntity newUser(Long id) {
		UserEntity user = newEntity(UserEntity.class);
		ReflectionTestUtils.setField(user, "id", id);
		ReflectionTestUtils.setField(user, "uuid", UUID.randomUUID());
		ReflectionTestUtils.setField(user, "status", UserStatus.ACTIVE);
		return user;
	}

	private CategoriesEntity newCategory(Long id, String name) {
		CategoriesEntity category = newEntity(CategoriesEntity.class);
		ReflectionTestUtils.setField(category, "id", id);
		ReflectionTestUtils.setField(category, "name", name);
		return category;
	}

	private PostsEntity newPost(Long id, UserEntity user, CategoriesEntity category) {
		PostsEntity post = PostsEntity.create(user, category, "title", "content", false, PostStatus.PUBLISHED);
		ReflectionTestUtils.setField(post, "id", id);
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
