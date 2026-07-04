package com.taxease.kafka.consumer;

import com.taxease.kafka.event.TaxEvent;
import com.taxease.tax.model.TaxAuditLog;
import com.taxease.tax.repository.TaxAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaxEventConsumer {

    private final TaxAuditLogRepository taxAuditLogRepository;

    @KafkaListener(
            topics = "${taxease.kafka.topics.tax-events:tax-events}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(
            @Payload TaxEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("Consumed TaxEvent type={} recordId={} userId={} partition={} offset={}",
                event.getEventType(), event.getTaxRecordId(), event.getUserId(), partition, offset);

        switch (event.getEventType()) {
            case TAX_RECORD_CREATED   -> handleCreated(event);
            case TAX_RECORD_SUBMITTED -> handleSubmitted(event);
            case TAX_RECORD_ACCEPTED  -> handleAccepted(event);
            case TAX_RECORD_REJECTED  -> handleRejected(event);
            default -> log.debug("No handler for event type {}", event.getEventType());
        }
    }

    @Transactional
    private void handleCreated(TaxEvent event) {
        log.info(">>> KAFKA CONSUMER: received TAX_RECORD_CREATED for user {}", event.getUserId());

        TaxAuditLog auditLog = TaxAuditLog.builder()
                .taxRecordId(event.getTaxRecordId())
                .userId(event.getUserId())
                .eventType(event.getEventType().name())
                .grossSalary(event.getGrossSalary())
                .recommendedRegime(event.getRecommendedRegime())
                .savings(event.getSavings())
                .processedAt(LocalDateTime.now())
                .build();

        taxAuditLogRepository.save(auditLog);
        log.info(">>> KAFKA CONSUMER: audit log saved for record {} (regime={}, savings={})",
                event.getTaxRecordId(), event.getRecommendedRegime(), event.getSavings());
    }

    private void handleSubmitted(TaxEvent event) {
        log.info("Tax record {} submitted for user {} year {}",
                event.getTaxRecordId(), event.getUserId(), event.getTaxYear());
        // TODO: trigger compliance checks, notify user
    }

    private void handleAccepted(TaxEvent event) {
        log.info("Tax record {} accepted for user {}", event.getTaxRecordId(), event.getUserId());
        // TODO: generate confirmation, update insight cache
    }

    private void handleRejected(TaxEvent event) {
        log.warn("Tax record {} rejected for user {}", event.getTaxRecordId(), event.getUserId());
        // TODO: notify user with rejection reason
    }
}
