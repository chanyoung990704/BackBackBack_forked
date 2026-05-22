package com.aivle.project.common.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxPublisher {

    private final OutboxRepository outboxRepository;
    private final ObjectProvider<KafkaTemplate<String, String>> kafkaTemplateProvider;

    @Scheduled(fixedDelay = 500)
    public void publishPendingEvents() {
        publishEventsBatch(50);
    }

    @Transactional
    public void publishEventsBatch(int limit) {
        List<OutboxEvent> pendingEvents = outboxRepository.findPendingEventsForPublishing(limit);
        if (pendingEvents.isEmpty()) {
            return;
        }

        KafkaTemplate<String, String> kafkaTemplate = kafkaTemplateProvider.getIfAvailable();
        if (kafkaTemplate == null) {
            log.warn("KafkaTemplate bean is not available. Skipping outbox events processing.");
            return;
        }

        for (OutboxEvent event : pendingEvents) {
            try {
                // Wait for the message delivery confirmation synchronously to ensure transaction consistency
                kafkaTemplate.send(event.getTopic(), event.getAggregateId(), event.getPayload())
                        .get(3, TimeUnit.SECONDS);

                event.setStatus(OutboxStatus.PROCESSED);
                log.info("Successfully published outbox event to topic: {}, id={}", event.getTopic(), event.getId());
            } catch (Exception e) {
                log.error("Failed to publish outbox event to topic: {}, id={}", event.getTopic(), event.getId(), e);
                event.setStatus(OutboxStatus.FAILED);
                event.incrementRetryCount();
            }
            outboxRepository.save(event);
        }
    }
}
