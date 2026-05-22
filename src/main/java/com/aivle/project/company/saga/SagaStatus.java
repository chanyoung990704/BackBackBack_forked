package com.aivle.project.company.saga;

/**
 * Saga 트랜잭션의 상태를 관리하는 Enum.
 */
public enum SagaStatus {
    STARTED,
    FINANCIAL_ANALYZED,
    NEWS_ANALYZED,
    AI_COMPLETED,
    FAILED,
    COMPENSATING,
    COMPENSATED
}
