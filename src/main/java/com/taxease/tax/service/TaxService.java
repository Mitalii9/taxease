package com.taxease.tax.service;

import com.taxease.tax.dto.CreateTaxRecordRequest;
import com.taxease.tax.dto.TaxRecordDTO;
import com.taxease.tax.model.TaxRecord;
import com.taxease.tax.model.TaxStatus;
import com.taxease.tax.repository.TaxRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaxService {

    private final TaxRecordRepository taxRecordRepository;

    public List<TaxRecordDTO> findByUser(String userId) {
        return taxRecordRepository.findByUserId(userId).stream().map(this::toDTO).toList();
    }

    public TaxRecordDTO findById(String id) {
        return taxRecordRepository.findById(id).map(this::toDTO)
                .orElseThrow(() -> new RuntimeException("Tax record not found: " + id));
    }

    @Transactional
    public TaxRecordDTO create(CreateTaxRecordRequest request) {
        BigDecimal taxableIncome = calculateTaxableIncome(request.getGrossIncome());
        BigDecimal taxLiability = calculateTaxLiability(taxableIncome);
        BigDecimal taxPaid = request.getTaxPaid() != null ? request.getTaxPaid() : BigDecimal.ZERO;
        BigDecimal refundOrDue = taxPaid.subtract(taxLiability);

        TaxRecord record = TaxRecord.builder()
                .userId(request.getUserId())
                .taxYear(request.getTaxYear())
                .grossIncome(request.getGrossIncome())
                .taxableIncome(taxableIncome)
                .taxLiability(taxLiability)
                .taxPaid(taxPaid)
                .refundOrDue(refundOrDue)
                .status(TaxStatus.DRAFT)
                .build();

        return toDTO(taxRecordRepository.save(record));
    }

    @Transactional
    public TaxRecordDTO updateStatus(String id, TaxStatus status) {
        TaxRecord record = taxRecordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tax record not found: " + id));
        record.setStatus(status);
        return toDTO(taxRecordRepository.save(record));
    }

    private BigDecimal calculateTaxableIncome(BigDecimal grossIncome) {
        // placeholder — real logic will apply deductions
        return grossIncome.multiply(BigDecimal.valueOf(0.85)).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateTaxLiability(BigDecimal taxableIncome) {
        // placeholder — real logic will apply slabs
        return taxableIncome.multiply(BigDecimal.valueOf(0.20)).setScale(2, RoundingMode.HALF_UP);
    }

    private TaxRecordDTO toDTO(TaxRecord r) {
        return TaxRecordDTO.builder()
                .id(r.getId())
                .userId(r.getUserId())
                .taxYear(r.getTaxYear())
                .grossIncome(r.getGrossIncome())
                .taxableIncome(r.getTaxableIncome())
                .taxLiability(r.getTaxLiability())
                .taxPaid(r.getTaxPaid())
                .refundOrDue(r.getRefundOrDue())
                .status(r.getStatus())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }
}
