package com.taxease.kafka.consumer;

import com.taxease.kafka.event.DocumentEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DocumentEventConsumer {

    @KafkaListener(
            topics = "${taxease.kafka.topics.document-events:document-events}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(
            @Payload DocumentEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("Consumed DocumentEvent type={} documentId={} userId={} partition={} offset={}",
                event.getEventType(), event.getDocumentId(), event.getUserId(), partition, offset);

        switch (event.getEventType()) {
            case DOCUMENT_UPLOADED   -> handleUploaded(event);
            case DOCUMENT_PROCESSED  -> handleProcessed(event);
            default -> log.debug("No handler for event type {}", event.getEventType());
        }
    }

    private void handleUploaded(DocumentEvent event) {
        log.info("Document {} uploaded by user {} type={}", event.getDocumentId(),
                event.getUserId(), event.getDocumentType());
        // TODO: trigger OCR / data extraction pipeline
    }

    private void handleProcessed(DocumentEvent event) {
        log.info("Document {} processed for user {}", event.getDocumentId(), event.getUserId());
        // TODO: update tax record with extracted data
    }
}
