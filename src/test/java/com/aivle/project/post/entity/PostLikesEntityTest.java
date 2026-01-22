package com.aivle.project.post;

import static org.assertj.core.api.Assertions.assertThat;

import com.aivle.project.category.entity.CategoriesEntity;
import com.aivle.project.common.config.TestSecurityConfig;
import com.aivle.project.post.entity.PostLikesEntity;
import com.aivle.project.post.entity.PostsEntity;
import com.aivle.project.user.entity.UserEntity;
import com.aivle.project.user.entity.UserStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
@Import(TestSecurityConfig.class)
class PostLikesEntityTest {

	private static final String DEFAULT_TITLE = "test title";
	private static final String DEFAULT_CONTENT = "test content";

	@PersistenceContext
	private EntityManager entityManager;

	@Test
	@DisplayName("post_likes는 post/user 연관관계를 저장하고 조회할 수 있다")
	void save_mapsPostAndUser() {
		UserEntity user = givenUser("like-owner@test.com", UserStatus.ACTIVE);
		CategoriesEntity category = givenCategory("general");
		entityManager.persist(user);
		entityManager.persist(category);

		PostsEntity post = givenPost(user, category);
		entityManager.persist(post);

		PostLikesEntity like = new PostLikesEntity(post, user);
		entityManager.persist(like);
		entityManager.flush();
		entityManager.clear();

		PostLikesEntity found = entityManager.find(PostLikesEntity.class, like.getId());

		assertThat(found.getPost().getId()).isEqualTo(post.getId());
		assertThat(found.getUser().getId()).isEqualTo(user.getId());
		assertThat(found.getCreatedAt()).isNotNull();
	}

	private UserEntity givenUser(String email, UserStatus status) {
		UserEntity user = newEntity(UserEntity.class);
		setField(user, "email", email);
		setField(user, "password", "encoded-password");
		setField(user, "name", "test-user");
		setField(user, "status", status);
		return user;
	}

	private CategoriesEntity givenCategory(String name) {
		CategoriesEntity category = newEntity(CategoriesEntity.class);
		setField(category, "name", name);
		return category;
	}

	private PostsEntity givenPost(UserEntity user, CategoriesEntity category) {
		PostsEntity post = newEntity(PostsEntity.class);
		setField(post, "user", user);
		setField(post, "category", category);
		setField(post, "title", DEFAULT_TITLE);
		setField(post, "content", DEFAULT_CONTENT);
		return post;
	}

	private <T> T newEntity(Class<T> type) {
		try {
			var ctor = type.getDeclaredConstructor();
			ctor.setAccessible(true);
			return ctor.newInstance();
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Failed to create " + type.getSimpleName(), e);
		}
	}

	private void setField(Object target, String fieldName, Object value) {
		ReflectionTestUtils.setField(target, fieldName, value);
	}
}
