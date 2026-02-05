package com.aivle.project.category.repository;

import com.aivle.project.category.entity.CategoriesEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 카테고리 조회/저장 리포지토리.
 */
public interface CategoriesRepository extends JpaRepository<CategoriesEntity, Long> {

	Optional<CategoriesEntity> findByIdAndDeletedAtIsNull(Long id);

	Optional<CategoriesEntity> findByNameAndDeletedAtIsNull(String name);

	List<CategoriesEntity> findAllByDeletedAtIsNullOrderBySortOrderAscIdAsc();
}
