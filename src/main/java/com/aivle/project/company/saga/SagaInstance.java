package com.aivle.project.company.saga;

import com.aivle.project.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Saga 상태를 추적하고 보존하는 JPA 엔티티 클래스.
 */
@Entity
@Table(name = "saga_instance")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SagaInstance extends BaseEntity {

    @Id
    @Column(name = "id", length = 100)
    private String id;

    @Column(name = "saga_type", length = 100, nullable = false)
    private String sagaType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50, nullable = false)
    private SagaStatus status;

    @Column(name = "current_step", length = 100, nullable = false)
    private String currentStep;

    @Column(name = "payload", columnDefinition = "TEXT", nullable = false)
    private String payload;

    public void transitionTo(SagaStatus newStatus, String nextStep) {
        this.status = newStatus;
        this.currentStep = nextStep;
    }
}
