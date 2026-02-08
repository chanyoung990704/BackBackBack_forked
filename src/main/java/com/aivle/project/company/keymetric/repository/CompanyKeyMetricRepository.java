package com.aivle.project.company.keymetric.repository;

import com.aivle.project.company.keymetric.entity.CompanyKeyMetricEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 기업 핵심 건강도 리포지토리.
 */
public interface CompanyKeyMetricRepository extends JpaRepository<CompanyKeyMetricEntity, Long> {

	Optional<CompanyKeyMetricEntity> findByCompanyIdAndQuarterId(Long companyId, Long quarterId);
}
