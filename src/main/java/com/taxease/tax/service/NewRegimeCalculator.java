package com.taxease.tax.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * FY 2025-26 new tax regime slabs with ₹75,000 standard deduction and 87A rebate up to ₹7L.
 */
@Service
public class NewRegimeCalculator {

    private static final BigDecimal STANDARD_DEDUCTION = BigDecimal.valueOf(75_000);
    private static final BigDecimal REBATE_LIMIT       = BigDecimal.valueOf(700_000);

    public NewRegimeResult calculate(BigDecimal grossSalary) {
        BigDecimal taxableIncome = grossSalary.subtract(STANDARD_DEDUCTION).max(BigDecimal.ZERO);
        BigDecimal rawTax = computeSlabTax(taxableIncome);

        // Rebate u/s 87A: taxable income ≤ ₹7L → full rebate, zero tax
        BigDecimal taxAfterRebate = taxableIncome.compareTo(REBATE_LIMIT) <= 0
                ? BigDecimal.ZERO : rawTax;

        BigDecimal cess = taxAfterRebate.multiply(BigDecimal.valueOf(0.04));
        BigDecimal totalTax = taxAfterRebate.add(cess).setScale(2, RoundingMode.HALF_UP);

        return new NewRegimeResult(totalTax, STANDARD_DEDUCTION, taxableIncome);
    }

    private BigDecimal computeSlabTax(BigDecimal income) {
        BigDecimal tax = BigDecimal.ZERO;

        // 3,00,001 – 7,00,000: 5%
        if (income.compareTo(BigDecimal.valueOf(300_000)) > 0) {
            BigDecimal slab = income.min(BigDecimal.valueOf(700_000))
                    .subtract(BigDecimal.valueOf(300_000));
            tax = tax.add(slab.multiply(BigDecimal.valueOf(0.05)));
        }
        // 7,00,001 – 10,00,000: 10%
        if (income.compareTo(BigDecimal.valueOf(700_000)) > 0) {
            BigDecimal slab = income.min(BigDecimal.valueOf(1_000_000))
                    .subtract(BigDecimal.valueOf(700_000));
            tax = tax.add(slab.multiply(BigDecimal.valueOf(0.10)));
        }
        // 10,00,001 – 12,00,000: 15%
        if (income.compareTo(BigDecimal.valueOf(1_000_000)) > 0) {
            BigDecimal slab = income.min(BigDecimal.valueOf(1_200_000))
                    .subtract(BigDecimal.valueOf(1_000_000));
            tax = tax.add(slab.multiply(BigDecimal.valueOf(0.15)));
        }
        // 12,00,001 – 15,00,000: 20%
        if (income.compareTo(BigDecimal.valueOf(1_200_000)) > 0) {
            BigDecimal slab = income.min(BigDecimal.valueOf(1_500_000))
                    .subtract(BigDecimal.valueOf(1_200_000));
            tax = tax.add(slab.multiply(BigDecimal.valueOf(0.20)));
        }
        // Above 15,00,000: 30%
        if (income.compareTo(BigDecimal.valueOf(1_500_000)) > 0) {
            BigDecimal slab = income.subtract(BigDecimal.valueOf(1_500_000));
            tax = tax.add(slab.multiply(BigDecimal.valueOf(0.30)));
        }

        return tax;
    }

    public record NewRegimeResult(
            BigDecimal taxAmount,
            BigDecimal standardDeduction,
            BigDecimal taxableIncome
    ) {}
}
