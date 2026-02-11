package com.aivle.project.category.entity;

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
class CategoriesEntityTest {

	@PersistenceContext
	private EntityManager entityManager;

	@Test
	@DisplayName("categories는 이름/설명을 저장하고 조회할 수 있다")
	void save_mapsNameAndDescription() {
		CategoriesEntity category = givenCategory("general", "general posts");

		entityManager.persist(category);
		entityManager.flush();
		entityManager.clear();

		CategoriesEntity found = entityManager.find(CategoriesEntity.class, category.getId());

		assertThat(found.getName()).isEqualTo("general");
		assertThat(found.getDescription()).isEqualTo("general posts");
	}

	@Test
	@DisplayName("categories 기본값이 저장 시 유지된다")
	void save_appliesDefaults() {
		CategoriesEntity category = givenCategory("defaults", null);

		entityManager.persist(category);
		entityManager.flush();
		entityManager.clear();

		CategoriesEntity found = entityManager.find(CategoriesEntity.class, category.getId());

		assertThat(found.getSortOrder()).isZero();
		assertThat(found.isActive()).isTrue();
	}

	private CategoriesEntity givenCategory(String name, String description) {
		CategoriesEntity category = newEntity(CategoriesEntity.class);
		setField(category, "name", name);
		setField(category, "description", description);
		return category;
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
