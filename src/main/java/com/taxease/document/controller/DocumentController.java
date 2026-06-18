package com.taxease.document.controller;

import com.taxease.document.dto.DocumentDTO;
import com.taxease.document.model.DocumentType;
import com.taxease.document.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
@Tag(name = "Documents", description = "Document upload and retrieval endpoints")
public class DocumentController {

    private final DocumentService documentService;

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get all documents for a user")
    public ResponseEntity<List<DocumentDTO>> findByUser(@PathVariable String userId) {
        return ResponseEntity.ok(documentService.findByUser(userId));
    }

    @GetMapping("/user/{userId}/year/{taxYear}")
    @Operation(summary = "Get documents for a user filtered by tax year")
    public ResponseEntity<List<DocumentDTO>> findByUserAndYear(
            @PathVariable String userId,
            @PathVariable Integer taxYear) {
        return ResponseEntity.ok(documentService.findByUserAndYear(userId, taxYear));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a document by ID")
    public ResponseEntity<DocumentDTO> findById(@PathVariable String id) {
        return ResponseEntity.ok(documentService.findById(id));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a tax document")
    public ResponseEntity<DocumentDTO> upload(
            @RequestParam String userId,
            @RequestParam MultipartFile file,
            @RequestParam DocumentType documentType,
            @RequestParam(required = false) Integer taxYear) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(documentService.upload(userId, file, documentType, taxYear));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a document")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        documentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
