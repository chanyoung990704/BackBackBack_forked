package com.aivle.project.industry.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.aivle.project.common.config.QuerydslConfig;
import com.aivle.project.company.entity.CompaniesEntity;
import com.aivle.project.company.repository.CompaniesRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
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
class IndustryRepositoryTest {

	@Autowired
	private IndustryRepository industryRepository;

	@Autowired
	private CompaniesRepository companiesRepository;

	@PersistenceContext
	private EntityManager entityManager;

	@AfterEach
	void tearDown() {
		companiesRepository.deleteAll();
		industryRepository.deleteAll();
		entityManager.flush();
		entityManager.clear();
	}

	@Test
	@DisplayName("업종 코드를 통해 업종 정보를 조회한다")
	void findByIndustryCode_shouldReturnEntity() {
		// given
		IndustryEntity industry = newEntity(IndustryEntity.class);
		ReflectionTestUtils.setField(industry, "industryCode", "99999");
		ReflectionTestUtils.setField(industry, "industryName", "테스트업종");
		entityManager.persist(industry);
		entityManager.flush();
		entityManager.clear();

		// when
		var result = industryRepository.findByIndustryCode("99999");

		// then
		assertThat(result).isPresent();
		assertThat(result.get().getIndustryName()).isEqualTo("테스트업종");
	}

	@Test
	@DisplayName("업종 코드가 없어도 기업 정보는 저장된다")
	void saveCompany_shouldAllowNullIndustryCode() {
		// given
		CompaniesEntity company = CompaniesEntity.create(
			"90000001",
			"테스트기업",
			"TEST_CO",
			"900001",
			LocalDate.of(2025, 1, 1),
			null
		);

		// when
		CompaniesEntity saved = companiesRepository.save(company);
		entityManager.flush();
		entityManager.clear();
		CompaniesEntity reloaded = companiesRepository.findById(saved.getId()).orElseThrow();

		// then
		assertThat(reloaded.getIndustryCode()).isNull();
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
