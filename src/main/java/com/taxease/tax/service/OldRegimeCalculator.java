package com.taxease.tax.service;

import com.taxease.tax.dto.TaxCalculationRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * FY 2025-26 old tax regime with standard deduction ₹50,000, HRA, 80C, 80D, Sec-24 deductions,
 * 87A rebate up to ₹5L taxable income, and 4% health & education cess.
 */
@Service
public class OldRegimeCalculator {

    private static final BigDecimal STANDARD_DEDUCTION  = BigDecimal.valueOf(50_000);
    private static final BigDecimal MAX_80C             = BigDecimal.valueOf(150_000);
    private static final BigDecimal MAX_80D             = BigDecimal.valueOf(50_000);
    private static final BigDecimal MAX_HOME_LOAN       = BigDecimal.valueOf(200_000);
    private static final BigDecimal REBATE_LIMIT        = BigDecimal.valueOf(500_000);

    public OldRegimeResult calculate(TaxCalculationRequest req) {
        BigDecimal hraExemption    = computeHraExemption(req);
        BigDecimal deduction80C    = req.getInvestment80C().min(MAX_80C);
        BigDecimal deduction80D    = req.getMedical80D().min(MAX_80D);
        BigDecimal homeLoan        = req.getHomeLoanInterest().min(MAX_HOME_LOAN);

        BigDecimal totalDeductions = STANDARD_DEDUCTION
                .add(hraExemption)
                .add(deduction80C)
                .add(deduction80D)
                .add(homeLoan);

        BigDecimal taxableIncome = req.getGrossSalary().subtract(totalDeductions)
                .max(BigDecimal.ZERO);
        BigDecimal rawTax = computeSlabTax(taxableIncome);

        // Rebate u/s 87A: taxable income ≤ ₹5L → full rebate, zero tax
        BigDecimal taxAfterRebate = taxableIncome.compareTo(REBATE_LIMIT) <= 0
                ? BigDecimal.ZERO : rawTax;

        BigDecimal cess = taxAfterRebate.multiply(BigDecimal.valueOf(0.04));
        BigDecimal totalTax = taxAfterRebate.add(cess).setScale(2, RoundingMode.HALF_UP);

        return new OldRegimeResult(
                totalTax, STANDARD_DEDUCTION, hraExemption,
                deduction80C, deduction80D, homeLoan, totalDeductions, taxableIncome
        );
    }

    /**
     * HRA exemption = min(actual HRA, rent paid − 10% basic, 50%/40% of basic for metro/non-metro).
     * Returns zero if no rent is being paid.
     */
    private BigDecimal computeHraExemption(TaxCalculationRequest req) {
        if (req.getRentPaid().compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal actualHra = req.getHraReceived();

        BigDecimal rentMinusTenPct = req.getRentPaid()
                .subtract(req.getBasicSalary().multiply(BigDecimal.valueOf(0.10)))
                .max(BigDecimal.ZERO);

        BigDecimal locationFactor = req.isMetroCity()
                ? BigDecimal.valueOf(0.50) : BigDecimal.valueOf(0.40);
        BigDecimal locationBased = req.getBasicSalary().multiply(locationFactor);

        return actualHra.min(rentMinusTenPct).min(locationBased).max(BigDecimal.ZERO);
    }

    private BigDecimal computeSlabTax(BigDecimal income) {
        BigDecimal tax = BigDecimal.ZERO;

        // 2,50,001 – 5,00,000: 5%
        if (income.compareTo(BigDecimal.valueOf(250_000)) > 0) {
            BigDecimal slab = income.min(BigDecimal.valueOf(500_000))
                    .subtract(BigDecimal.valueOf(250_000));
            tax = tax.add(slab.multiply(BigDecimal.valueOf(0.05)));
        }
        // 5,00,001 – 10,00,000: 20%
        if (income.compareTo(BigDecimal.valueOf(500_000)) > 0) {
            BigDecimal slab = income.min(BigDecimal.valueOf(1_000_000))
                    .subtract(BigDecimal.valueOf(500_000));
            tax = tax.add(slab.multiply(BigDecimal.valueOf(0.20)));
        }
        // Above 10,00,000: 30%
        if (income.compareTo(BigDecimal.valueOf(1_000_000)) > 0) {
            BigDecimal slab = income.subtract(BigDecimal.valueOf(1_000_000));
            tax = tax.add(slab.multiply(BigDecimal.valueOf(0.30)));
        }

        return tax;
    }

    public record OldRegimeResult(
            BigDecimal taxAmount,
            BigDecimal standardDeduction,
            BigDecimal hraExemption,
            BigDecimal deduction80C,
            BigDecimal deduction80D,
            BigDecimal homeLoanInterest,
            BigDecimal totalDeductions,
            BigDecimal taxableIncome
    ) {}
}
