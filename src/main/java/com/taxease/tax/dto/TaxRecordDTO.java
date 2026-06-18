package com.taxease.tax.dto;

import com.taxease.tax.model.TaxStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class TaxRecordDTO {
    private String id;
    private String userId;
    private Integer taxYear;
    private BigDecimal grossIncome;
    private BigDecimal taxableIncome;
    private BigDecimal taxLiability;
    private BigDecimal taxPaid;
    private BigDecimal refundOrDue;
    private TaxStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
