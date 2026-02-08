package com.aivle.project.comment.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.aivle.project.category.entity.CategoriesEntity;
import com.aivle.project.comment.entity.CommentsEntity;
import com.aivle.project.common.config.QuerydslConfig;
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
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;

@DataJpaTest
@Import(QuerydslConfig.class)
class CommentsRepositoryTest {

	@Autowired
	private CommentsRepository commentsRepository;

	@PersistenceContext
	private EntityManager entityManager;

	@Test
	@DisplayName("댓글을 depth/sequence 순으로 조회한다")
	void findByPostIdOrderByDepthAscSequenceAsc_shouldReturnOrdered() {
		// given
		UserEntity user = persistUser("comments@test.com");
		CategoriesEntity category = persistCategory("comments");
		PostsEntity post = persistPost(user, category, "title", "content");

		CommentsEntity parent = CommentsEntity.create(post, user, null, "parent", 0, 0);
		CommentsEntity replyOne = CommentsEntity.create(post, user, parent, "reply-1", 1, 0);
		CommentsEntity replyTwo = CommentsEntity.create(post, user, parent, "reply-2", 1, 1);

		entityManager.persist(parent);
		entityManager.persist(replyOne);
		entityManager.persist(replyTwo);
		entityManager.flush();

		// when
		List<CommentsEntity> results = commentsRepository.findByPostIdOrderByDepthAscSequenceAsc(post.getId());

		// then
		assertThat(results).hasSize(3);
		assertThat(results.get(0).getContent()).isEqualTo("parent");
		assertThat(results.get(1).getContent()).isEqualTo("reply-1");
		assertThat(results.get(2).getContent()).isEqualTo("reply-2");
	}

	@Test
	@DisplayName("부모 댓글의 최대 sequence를 조회한다")
	void findMaxSequenceByParentId_shouldReturnMax() {
		// given
		UserEntity user = persistUser("sequence@test.com");
		CategoriesEntity category = persistCategory("sequence");
		PostsEntity post = persistPost(user, category, "title", "content");

		CommentsEntity parent = CommentsEntity.create(post, user, null, "parent", 0, 0);
		entityManager.persist(parent);
		entityManager.flush();

		CommentsEntity replyOne = CommentsEntity.create(post, user, parent, "reply-1", 1, 0);
		CommentsEntity replyTwo = CommentsEntity.create(post, user, parent, "reply-2", 1, 2);
		entityManager.persist(replyOne);
		entityManager.persist(replyTwo);
		entityManager.flush();

		// when
		int max = commentsRepository.findMaxSequenceByParentId(parent.getId());

		// then
		assertThat(max).isEqualTo(2);
	}

	@Test
	@DisplayName("최상위 댓글의 최대 sequence를 조회한다")
	void findMaxSequenceByPostIdAndParentIsNull_shouldReturnMax() {
		// given
		UserEntity user = persistUser("top@test.com");
		CategoriesEntity category = persistCategory("top");
		PostsEntity post = persistPost(user, category, "title", "content");

		CommentsEntity first = CommentsEntity.create(post, user, null, "first", 0, 0);
		CommentsEntity second = CommentsEntity.create(post, user, null, "second", 0, 3);
		entityManager.persist(first);
		entityManager.persist(second);
		entityManager.flush();

		// when
		int max = commentsRepository.findMaxSequenceByPostIdAndParentIsNull(post.getId());

		// then
		assertThat(max).isEqualTo(3);
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
