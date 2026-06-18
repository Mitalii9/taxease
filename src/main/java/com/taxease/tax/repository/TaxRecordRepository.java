package com.taxease.tax.repository;

import com.taxease.tax.model.TaxRecord;
import com.taxease.tax.model.TaxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaxRecordRepository extends JpaRepository<TaxRecord, String> {

    List<TaxRecord> findByUserId(String userId);

    Optional<TaxRecord> findByUserIdAndTaxYear(String userId, Integer taxYear);

    List<TaxRecord> findByStatus(TaxStatus status);
}
