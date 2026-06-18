package com.taxease.document.service;

import com.taxease.document.dto.DocumentDTO;
import com.taxease.document.model.Document;
import com.taxease.document.model.DocumentType;
import com.taxease.document.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DocumentService {

    private final DocumentRepository documentRepository;

    public List<DocumentDTO> findByUser(String userId) {
        return documentRepository.findByUserId(userId).stream().map(this::toDTO).toList();
    }

    public List<DocumentDTO> findByUserAndYear(String userId, Integer taxYear) {
        return documentRepository.findByUserIdAndTaxYear(userId, taxYear).stream().map(this::toDTO).toList();
    }

    public DocumentDTO findById(String id) {
        return documentRepository.findById(id).map(this::toDTO)
                .orElseThrow(() -> new RuntimeException("Document not found: " + id));
    }

    @Transactional
    public DocumentDTO upload(String userId, MultipartFile file, DocumentType documentType, Integer taxYear) {
        // placeholder — real implementation would upload to S3/GCS and store the key
        String storageKey = "documents/" + userId + "/" + UUID.randomUUID() + "_" + file.getOriginalFilename();

        Document document = Document.builder()
                .userId(userId)
                .fileName(file.getOriginalFilename())
                .contentType(file.getContentType())
                .fileSizeBytes(file.getSize())
                .storageKey(storageKey)
                .documentType(documentType)
                .taxYear(taxYear)
                .build();

        return toDTO(documentRepository.save(document));
    }

    @Transactional
    public void delete(String id) {
        documentRepository.deleteById(id);
    }

    private DocumentDTO toDTO(Document d) {
        return DocumentDTO.builder()
                .id(d.getId())
                .userId(d.getUserId())
                .fileName(d.getFileName())
                .contentType(d.getContentType())
                .fileSizeBytes(d.getFileSizeBytes())
                .documentType(d.getDocumentType())
                .taxYear(d.getTaxYear())
                .uploadedAt(d.getUploadedAt())
                .build();
    }
}
