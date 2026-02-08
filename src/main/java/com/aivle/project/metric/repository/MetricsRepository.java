package com.aivle.project.metric.repository;

import com.aivle.project.metric.entity.MetricsEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 지표 조회/저장 리포지토리.
 */
public interface MetricsRepository extends JpaRepository<MetricsEntity, Long> {

	Optional<MetricsEntity> findByMetricCode(String metricCode);

	List<MetricsEntity> findAllByMetricCodeIn(Collection<String> metricCodes);

	List<MetricsEntity> findAllByMetricNameEnIn(Collection<String> metricNameEns);
}
