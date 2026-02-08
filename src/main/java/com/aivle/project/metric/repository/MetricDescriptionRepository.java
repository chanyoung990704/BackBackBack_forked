package com.aivle.project.metric.repository;

import com.aivle.project.metric.entity.MetricDescriptionEntity;
import com.aivle.project.metric.entity.MetricsEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 지표 설명 리포지토리.
 */
public interface MetricDescriptionRepository extends JpaRepository<MetricDescriptionEntity, Long> {

	Optional<MetricDescriptionEntity> findByMetricAndLocale(MetricsEntity metric, String locale);
}
