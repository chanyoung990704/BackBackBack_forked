package com.aivle.project.tag.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.aivle.project.common.config.TestSecurityConfig;
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
class TagsEntityTest {

	@PersistenceContext
	private EntityManager entityManager;

	@Test
	@DisplayName("tags는 name을 저장하고 조회할 수 있다")
	void save_mapsName() {
		TagsEntity tag = givenTag("spring");

		entityManager.persist(tag);
		entityManager.flush();
		entityManager.clear();

		TagsEntity found = entityManager.find(TagsEntity.class, tag.getId());

		assertThat(found.getName()).isEqualTo("spring");
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
