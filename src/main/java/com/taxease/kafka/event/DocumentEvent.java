package com.taxease.kafka.event;

import com.taxease.document.model.DocumentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentEvent {

    private String eventId;
    private EventType eventType;
    private String documentId;
    private String userId;
    private DocumentType documentType;
    private Integer taxYear;
    private LocalDateTime occurredAt;

    public enum EventType {
        DOCUMENT_UPLOADED,
        DOCUMENT_DELETED,
        DOCUMENT_PROCESSED
    }
}
