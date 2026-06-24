package com.taxease.tax.dto;

import com.taxease.tax.model.enums.Regime;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class TaxRecordDTO {
    private String id;
    private String userId;
    private BigDecimal grossSalary;
    private Regime regimeChosen;
    private BigDecimal taxOldRegime;
    private BigDecimal taxNewRegime;
    private Regime recommendedRegime;
    private BigDecimal savings;
    private LocalDateTime calculatedAt;
}
