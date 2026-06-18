package com.taxease.document.dto;

import com.taxease.document.model.DocumentType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DocumentDTO {
    private String id;
    private String userId;
    private String fileName;
    private String contentType;
    private Long fileSizeBytes;
    private DocumentType documentType;
    private Integer taxYear;
    private LocalDateTime uploadedAt;
}
