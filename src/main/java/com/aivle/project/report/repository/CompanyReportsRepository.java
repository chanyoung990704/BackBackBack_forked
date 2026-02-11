package com.aivle.project.report.repository;

import com.aivle.project.report.entity.CompanyReportsEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

/**
 * 회사 보고서 조회/저장 리포지토리.
 */
public interface CompanyReportsRepository extends JpaRepository<CompanyReportsEntity, Long> {

	Optional<CompanyReportsEntity> findByCompanyIdAndQuarterId(Long companyId, Long quarterId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select cr from CompanyReportsEntity cr where cr.id = :id")
	Optional<CompanyReportsEntity> findByIdForUpdate(@Param("id") Long id);
}
