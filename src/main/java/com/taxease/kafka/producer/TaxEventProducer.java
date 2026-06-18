package com.taxease.kafka.producer;

import com.taxease.kafka.event.TaxEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaxEventProducer {

    private final KafkaTemplate<String, TaxEvent> kafkaTemplate;

    @Value("${taxease.kafka.topics.tax-events:tax-events}")
    private String topic;

    public void publish(TaxEvent event) {
        CompletableFuture<SendResult<String, TaxEvent>> future =
                kafkaTemplate.send(topic, event.getTaxRecordId(), event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish TaxEvent {} for record {}: {}",
                        event.getEventType(), event.getTaxRecordId(), ex.getMessage());
            } else {
                log.debug("Published TaxEvent {} for record {} to partition {}",
                        event.getEventType(), event.getTaxRecordId(),
                        result.getRecordMetadata().partition());
            }
        });
    }
}
