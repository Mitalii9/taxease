package com.taxease.tax.repository;

import com.taxease.tax.model.TaxRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaxRecordRepository extends JpaRepository<TaxRecord, String> {

    List<TaxRecord> findByUserId(String userId);
}
