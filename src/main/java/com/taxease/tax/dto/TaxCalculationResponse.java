package com.taxease.tax.dto;

import com.taxease.tax.model.enums.Regime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaxCalculationResponse {

    private BigDecimal grossSalary;
    private BigDecimal taxOldRegime;
    private BigDecimal taxNewRegime;
    private Regime recommendedRegime;
    private BigDecimal savings;
    private DeductionBreakdown deductions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeductionBreakdown {

        // Old regime deductions
        private BigDecimal standardDeductionOld;
        private BigDecimal hraExemption;
        private BigDecimal deduction80C;
        private BigDecimal deduction80D;
        private BigDecimal homeLoanInterestSec24;
        private BigDecimal totalOldRegimeDeductions;
        private BigDecimal taxableIncomeOldRegime;

        // New regime
        private BigDecimal standardDeductionNew;
        private BigDecimal taxableIncomeNewRegime;
    }
}
