package com.aivle.project.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.aivle.project.category.entity.CategoriesEntity;
import com.aivle.project.category.repository.CategoriesRepository;
import com.aivle.project.common.dto.PageRequest;
import com.aivle.project.common.dto.PageResponse;
import com.aivle.project.common.error.CommonException;
import com.aivle.project.post.dto.PostCreateRequest;
import com.aivle.project.post.dto.PostResponse;
import com.aivle.project.post.dto.PostUpdateRequest;
import com.aivle.project.post.entity.PostStatus;
import com.aivle.project.post.entity.PostsEntity;
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
	private CategoriesRepository categoriesRepository;

	@Test
	@DisplayName("페이징 요청으로 게시글 목록을 조회한다")
	void list_shouldReturnPageResponse() {
		// given
		PageRequest pageRequest = new PageRequest();
		pageRequest.setPage(1);
		pageRequest.setSize(2);
		pageRequest.setSortBy("createdAt");

		UserEntity user = newUser(1L, UUID.randomUUID());
		CategoriesEntity category = newCategory(10L);
		PostsEntity first = newPost(100L, user, category, "title-1", "content-1");
		PostsEntity second = newPost(101L, user, category, "title-2", "content-2");
		Page<PostsEntity> page = new PageImpl<>(List.of(first, second), pageRequest.toPageable(), 2);

		given(postsRepository.findAllByDeletedAtIsNullOrderByCreatedAtDesc(any(Pageable.class)))
			.willReturn(page);

		// when
		PageResponse<PostResponse> response = postService.list(pageRequest);

		// then
		assertThat(response.totalElements()).isEqualTo(2);
		assertThat(response.content()).hasSize(2);
		assertThat(response.content().get(0).id()).isEqualTo(100L);
		assertThat(response.content().get(1).id()).isEqualTo(101L);
	}

	@Test
	@DisplayName("게시글 생성 시 제목과 내용이 trim 처리된다")
	void create_shouldTrimFields() {
		// given
		UserEntity user = newUser(1L, UUID.randomUUID());
		CategoriesEntity category = newCategory(10L);

		PostCreateRequest request = new PostCreateRequest();
		request.setCategoryId(10L);
		request.setTitle(" 제목 ");
		request.setContent(" 내용 ");

		given(categoriesRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(category));
		given(postsRepository.save(any(PostsEntity.class))).willAnswer(invocation -> {
			PostsEntity saved = invocation.getArgument(0);
			ReflectionTestUtils.setField(saved, "id", 100L);
			return saved;
		});

		// when
		PostResponse response = postService.create(user, request);

		// then
		assertThat(response.id()).isEqualTo(100L);
		assertThat(response.title()).isEqualTo("제목");
		assertThat(response.content()).isEqualTo("내용");
		assertThat(response.userId()).isEqualTo(1L);
		assertThat(response.categoryId()).isEqualTo(10L);
	}

	@Test
	@DisplayName("게시글 수정 시 작성자 검증과 필드 변경이 반영된다")
	void update_shouldApplyChanges() {
		// given
		UserEntity user = newUser(1L, UUID.randomUUID());
		CategoriesEntity category = newCategory(10L);
		CategoriesEntity nextCategory = newCategory(20L);
		PostsEntity post = newPost(100L, user, category, "before", "before");

		PostUpdateRequest request = new PostUpdateRequest();
		request.setTitle(" after ");
		request.setContent(" after ");
		request.setCategoryId(20L);

		given(postsRepository.findByIdAndDeletedAtIsNull(100L)).willReturn(Optional.of(post));
		given(categoriesRepository.findByIdAndDeletedAtIsNull(20L)).willReturn(Optional.of(nextCategory));

		// when
		PostResponse response = postService.update(user, 100L, request);

		// then
		assertThat(response.title()).isEqualTo("after");
		assertThat(response.content()).isEqualTo("after");
		assertThat(response.categoryId()).isEqualTo(20L);
	}

	@Test
	@DisplayName("작성자가 아니면 게시글 수정이 실패한다")
	void update_shouldFailWhenNotOwner() {
		// given
		UserEntity requester = newUser(1L, UUID.randomUUID());
		UserEntity owner = newUser(2L, UUID.randomUUID());
		CategoriesEntity category = newCategory(10L);
		PostsEntity post = newPost(100L, owner, category, "title", "content");

		PostUpdateRequest request = new PostUpdateRequest();
		request.setTitle("changed");

		given(postsRepository.findByIdAndDeletedAtIsNull(100L)).willReturn(Optional.of(post));

		// when & then
		assertThatThrownBy(() -> postService.update(requester, 100L, request))
			.isInstanceOf(CommonException.class);
	}

	@Test
	@DisplayName("게시글 삭제 시 소프트 삭제가 반영된다")
	void delete_shouldMarkDeleted() {
		// given
		UserEntity user = newUser(1L, UUID.randomUUID());
		CategoriesEntity category = newCategory(10L);
		PostsEntity post = newPost(100L, user, category, "title", "content");

		given(postsRepository.findByIdAndDeletedAtIsNull(100L)).willReturn(Optional.of(post));

		// when
		postService.delete(user, 100L);

		// then
		assertThat(post.isDeleted()).isTrue();
		assertThat(post.getUpdatedBy()).isEqualTo(1L);
		verify(postsRepository).findByIdAndDeletedAtIsNull(100L);
	}

	private UserEntity newUser(Long id, UUID uuid) {
		UserEntity user = newEntity(UserEntity.class);
		ReflectionTestUtils.setField(user, "id", id);
		ReflectionTestUtils.setField(user, "uuid", uuid);
		ReflectionTestUtils.setField(user, "status", UserStatus.ACTIVE);
		return user;
	}

	private CategoriesEntity newCategory(Long id) {
		CategoriesEntity category = newEntity(CategoriesEntity.class);
		ReflectionTestUtils.setField(category, "id", id);
		ReflectionTestUtils.setField(category, "name", "category");
		return category;
	}

	private PostsEntity newPost(Long id, UserEntity user, CategoriesEntity category, String title, String content) {
		PostsEntity post = PostsEntity.create(user, category, title, content, false, PostStatus.PUBLISHED, user.getId());
		ReflectionTestUtils.setField(post, "id", id);
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
