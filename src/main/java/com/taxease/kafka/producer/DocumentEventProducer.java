package com.taxease.kafka.producer;

import com.taxease.kafka.event.DocumentEvent;
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
public class DocumentEventProducer {

    private final KafkaTemplate<String, DocumentEvent> kafkaTemplate;

    @Value("${taxease.kafka.topics.document-events:document-events}")
    private String topic;

    public void publish(DocumentEvent event) {
        CompletableFuture<SendResult<String, DocumentEvent>> future =
                kafkaTemplate.send(topic, event.getDocumentId(), event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish DocumentEvent {} for document {}: {}",
                        event.getEventType(), event.getDocumentId(), ex.getMessage());
            } else {
                log.debug("Published DocumentEvent {} for document {} to partition {}",
                        event.getEventType(), event.getDocumentId(),
                        result.getRecordMetadata().partition());
            }
        });
    }
}
