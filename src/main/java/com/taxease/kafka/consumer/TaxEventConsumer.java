package com.taxease.kafka.consumer;

import com.taxease.kafka.event.TaxEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TaxEventConsumer {

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
            case TAX_RECORD_SUBMITTED -> handleSubmitted(event);
            case TAX_RECORD_ACCEPTED  -> handleAccepted(event);
            case TAX_RECORD_REJECTED  -> handleRejected(event);
            default -> log.debug("No handler for event type {}", event.getEventType());
        }
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
