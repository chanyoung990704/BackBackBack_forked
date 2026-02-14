package com.aivle.project.post.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.aivle.project.category.entity.CategoriesEntity;
import com.aivle.project.common.util.NameMaskingUtil;
import com.aivle.project.post.dto.PostResponse;
import com.aivle.project.post.entity.PostStatus;
import com.aivle.project.post.entity.PostsEntity;
import com.aivle.project.user.entity.UserEntity;
import com.aivle.project.user.entity.UserStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class PostMapperTest {

	private final PostMapper postMapper = new PostMapperImpl();

	@Test
	@DisplayName("QnA 상태를 지정해서 응답에 주입할 수 있다")
	void toResponseWithQnaStatus_shouldInjectGivenStatus() {
		// given
		CategoriesEntity qnaCategory = mock(CategoriesEntity.class);
		given(qnaCategory.getName()).willReturn("qna");
		given(qnaCategory.getId()).willReturn(2L);

		UserEntity author = UserEntity.create("user@test.com", "pw", "user", null, UserStatus.ACTIVE);
		ReflectionTestUtils.setField(author, "id", 1L);

		PostsEntity post = PostsEntity.create(author, qnaCategory, "Title", "Content", false, PostStatus.PUBLISHED);
		ReflectionTestUtils.setField(post, "id", 100L);

		// when
		PostResponse response = postMapper.toResponseWithQnaStatus(post, "pending");

		// then
		assertThat(response.qnaStatus()).isEqualTo("pending");
		assertThat(response.name()).isEqualTo(NameMaskingUtil.mask("user"));
	}

	@Test
	@DisplayName("QnA 상태를 answered로 지정하면 응답에 반영된다")
	void toResponseWithQnaStatus_shouldReturnAnswered() {
		// given
		CategoriesEntity qnaCategory = mock(CategoriesEntity.class);
		given(qnaCategory.getName()).willReturn("qna");
		given(qnaCategory.getId()).willReturn(2L);

		UserEntity author = UserEntity.create("user@test.com", "pw", "user", null, UserStatus.ACTIVE);
		ReflectionTestUtils.setField(author, "id", 1L);

		PostsEntity post = PostsEntity.create(author, qnaCategory, "Title", "Content", false, PostStatus.PUBLISHED);
		ReflectionTestUtils.setField(post, "id", 100L);

		// when
		PostResponse response = postMapper.toResponseWithQnaStatus(post, "answered");

		// then
		assertThat(response.qnaStatus()).isEqualTo("answered");
		assertThat(response.name()).isEqualTo(NameMaskingUtil.mask("user"));
	}

	@Test
	@DisplayName("기본 게시글 매핑은 qnaStatus를 null로 둔다")
	void toResponse_shouldKeepQnaStatusNull() {
		// given
		CategoriesEntity noticeCategory = mock(CategoriesEntity.class);
		given(noticeCategory.getName()).willReturn("notices");
		given(noticeCategory.getId()).willReturn(1L);

		UserEntity author = UserEntity.create("user@test.com", "pw", "user", null, UserStatus.ACTIVE);
		PostsEntity post = PostsEntity.create(author, noticeCategory, "Title", "Content", false, PostStatus.PUBLISHED);

		// when
		PostResponse response = postMapper.toResponse(post);

		// then
		assertThat(response.qnaStatus()).isNull();
		assertThat(response.name()).isEqualTo(NameMaskingUtil.mask("user"));
	}
}
