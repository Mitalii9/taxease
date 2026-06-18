package com.taxease.document.repository;

import com.taxease.document.model.Document;
import com.taxease.document.model.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, String> {

    List<Document> findByUserId(String userId);

    List<Document> findByUserIdAndTaxYear(String userId, Integer taxYear);

    List<Document> findByUserIdAndDocumentType(String userId, DocumentType documentType);
}
