package com.aivle.project.post.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.aivle.project.category.entity.CategoriesEntity;
import com.aivle.project.common.config.TestSecurityConfig;
import com.aivle.project.tag.entity.TagsEntity;
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
@ActiveProfiles("test")
@Transactional
@Import(TestSecurityConfig.class)
class PostTagsEntityTest {

	private static final String DEFAULT_TITLE = "test title";
	private static final String DEFAULT_CONTENT = "test content";

	@PersistenceContext
	private EntityManager entityManager;

	@Test
	@DisplayName("post_tags는 post/tag 연관관계를 저장하고 조회할 수 있다")
	void save_mapsPostAndTag() {
		UserEntity user = givenUser("tag-owner@test.com", UserStatus.ACTIVE);
		CategoriesEntity category = givenCategory("general");
		entityManager.persist(user);
		entityManager.persist(category);

		PostsEntity post = givenPost(user, category);
		entityManager.persist(post);

		TagsEntity tag = givenTag("spring");
		entityManager.persist(tag);

		PostTagsEntity postTag = new PostTagsEntity(post, tag);
		entityManager.persist(postTag);
		entityManager.flush();
		entityManager.clear();

		PostTagsEntity found = entityManager.find(PostTagsEntity.class, postTag.getId());

		assertThat(found.getPost().getId()).isEqualTo(post.getId());
		assertThat(found.getTag().getId()).isEqualTo(tag.getId());
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

	private TagsEntity givenTag(String name) {
		TagsEntity tag = newEntity(TagsEntity.class);
		setField(tag, "name", name);
		return tag;
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
