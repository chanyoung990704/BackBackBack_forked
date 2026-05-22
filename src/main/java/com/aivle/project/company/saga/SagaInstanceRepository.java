package com.aivle.project.company.saga;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * SagaInstance를 처리하는 JPA Repository 인터페이스.
 */
@Repository
public interface SagaInstanceRepository extends JpaRepository<SagaInstance, String> {
}
