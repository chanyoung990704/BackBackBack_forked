package com.aivle.project.metricaverage.repository;

import com.aivle.project.metricaverage.entity.MetricAverageEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 분기별 지표 평균 집계 리포지토리.
 */
public interface MetricAverageRepository extends JpaRepository<MetricAverageEntity, Long> {

	Optional<MetricAverageEntity> findByQuarterIdAndMetricId(Long quarterId, Long metricId);

	boolean existsByQuarterIdAndMetricId(Long quarterId, Long metricId);
}
