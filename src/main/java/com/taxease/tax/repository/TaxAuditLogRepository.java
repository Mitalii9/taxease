package com.taxease.tax.repository;

import com.taxease.tax.model.TaxAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaxAuditLogRepository extends JpaRepository<TaxAuditLog, String> {
}
