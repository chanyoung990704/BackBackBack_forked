package com.aivle.project.post.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.aivle.project.common.config.QuerydslConfig;
import com.aivle.project.category.entity.CategoriesEntity;
import com.aivle.project.post.entity.PostStatus;
import com.aivle.project.post.entity.PostsEntity;
import com.aivle.project.user.entity.UserEntity;
import com.aivle.project.user.entity.UserStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

@ActiveProfiles("test")
@DataJpaTest
@Import(QuerydslConfig.class)
class PostsRepositoryTest {

	@Autowired
	private PostsRepository postsRepository;

	@PersistenceContext
	private EntityManager entityManager;

	@Test
	@DisplayName("삭제되지 않은 게시글만 최신순으로 조회한다")
	void findAllByDeletedAtIsNullOrderByCreatedAtDesc_shouldExcludeDeleted() {
		// given
		UserEntity user = persistUser("repo@test.com");
		CategoriesEntity category = persistCategory("repo");

		PostsEntity first = newPost(user, category, "first", "content");
		PostsEntity second = newPost(user, category, "second", "content");
		PostsEntity third = newPost(user, category, "third", "content");

		entityManager.persist(first);
		entityManager.persist(second);
		entityManager.persist(third);
		entityManager.flush();

		// JPA Auditing을 우회하여 과거 시점으로 강제 업데이트
		updateCreatedAt(first.getId(), LocalDateTime.now().minusDays(3));
		updateCreatedAt(second.getId(), LocalDateTime.now().minusDays(2));
		updateCreatedAt(third.getId(), LocalDateTime.now().minusDays(1));

		// 영속성 컨텍스트를 비워 DB의 변경 사항을 반영
		entityManager.clear();

		// when
		PostsEntity target = entityManager.find(PostsEntity.class, second.getId());
		target.markDeleted();
		entityManager.flush();
		entityManager.clear();

		var results = postsRepository.findAllByDeletedAtIsNullOrderByCreatedAtDesc();

		// then
		assertThat(results)
			.extracting(PostsEntity::getTitle)
			.containsExactly("third", "first");
	}

	private void updateCreatedAt(Long id, LocalDateTime createdAt) {
		entityManager.createNativeQuery("UPDATE posts SET created_at = ?1, updated_at = ?2 WHERE id = ?3")
			.setParameter(1, createdAt)
			.setParameter(2, createdAt)
			.setParameter(3, id)
			.executeUpdate();
	}

	@Test
	@DisplayName("삭제된 게시글은 id 조회에서 제외된다")
	void findByIdAndDeletedAtIsNull_shouldReturnEmptyForDeleted() {
		// given
		UserEntity user = persistUser("repo2@test.com");
		CategoriesEntity category = persistCategory("repo2");
		PostsEntity post = newPost(user, category, "title", "content");
		entityManager.persist(post);
		entityManager.flush();

		// when
		post.markDeleted();
		entityManager.flush();

		// then
		assertThat(postsRepository.findByIdAndDeletedAtIsNull(post.getId())).isEmpty();
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

	private PostsEntity newPost(UserEntity user, CategoriesEntity category, String title, String content) {
		return PostsEntity.create(user, category, title, content, false, PostStatus.PUBLISHED);
	}

	private void setCreatedAt(PostsEntity post, LocalDateTime createdAt) {
		ReflectionTestUtils.setField(post, "createdAt", createdAt);
		ReflectionTestUtils.setField(post, "updatedAt", createdAt);
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
