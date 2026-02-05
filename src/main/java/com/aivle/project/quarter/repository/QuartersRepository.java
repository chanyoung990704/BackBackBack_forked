package com.aivle.project.quarter.repository;

import com.aivle.project.quarter.entity.QuartersEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 분기 조회/저장 리포지토리.
 */
public interface QuartersRepository extends JpaRepository<QuartersEntity, Long> {

	Optional<QuartersEntity> findByQuarterKey(int quarterKey);

	Optional<QuartersEntity> findByYearAndQuarter(short year, byte quarter);
}
