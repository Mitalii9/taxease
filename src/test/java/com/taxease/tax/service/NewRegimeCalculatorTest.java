package com.taxease.tax.service;

import com.taxease.tax.service.NewRegimeCalculator.NewRegimeResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class NewRegimeCalculatorTest {

    private final NewRegimeCalculator calculator = new NewRegimeCalculator();

    @Test
    @DisplayName("Taxable income exactly at 7L rebate limit → 87A rebate wipes tax to zero")
    void belowRebateLimit_taxableSevenLakh_returnsZeroTax() {
        // gross 7,75,000 → taxable = 7,75,000 − 75,000 = 7,00,000 ≤ rebate limit
        NewRegimeResult result = calculator.calculate(BigDecimal.valueOf(775_000));

        assertThat(result.taxAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Gross 12L → 71,500 after slab tax + 4% cess")
    void grossTwelveLakh_returnsCorrectTaxWithCess() {
        // taxable = 12,00,000 − 75,000 = 11,25,000  (no rebate: 11,25,000 > 7,00,000)
        // 5%  on 4,00,000 (300k–700k)   = 20,000
        // 10% on 3,00,000 (700k–1000k)  = 30,000
        // 15% on 1,25,000 (1000k–1125k) = 18,750
        // raw = 68,750  →  cess 2,750  →  total 71,500
        NewRegimeResult result = calculator.calculate(BigDecimal.valueOf(1_200_000));

        assertThat(result.taxAmount()).isEqualByComparingTo(BigDecimal.valueOf(71_500));
    }

    @Test
    @DisplayName("Gross 20L reaches 30% slab → total tax 2,78,200")
    void grossTwentyLakh_hitsThirtyPercentSlab() {
        // taxable = 20,00,000 − 75,000 = 19,25,000
        // 5%  on 4,00,000 = 20,000
        // 10% on 3,00,000 = 30,000
        // 15% on 2,00,000 = 30,000
        // 20% on 3,00,000 = 60,000
        // 30% on 4,25,000 = 1,27,500
        // raw = 2,67,500  →  cess 10,700  →  total 2,78,200
        NewRegimeResult result = calculator.calculate(BigDecimal.valueOf(2_000_000));

        assertThat(result.taxAmount()).isEqualByComparingTo(BigDecimal.valueOf(278_200));
    }

    @Test
    @DisplayName("Standard deduction of 75,000 is always applied regardless of gross")
    void standardDeduction_isAlwaysSeventyFiveThousand() {
        // gross 10,00,000 → taxable should be 9,25,000 exactly
        BigDecimal gross = BigDecimal.valueOf(1_000_000);

        NewRegimeResult result = calculator.calculate(gross);

        assertThat(result.standardDeduction()).isEqualByComparingTo(BigDecimal.valueOf(75_000));
        assertThat(result.taxableIncome()).isEqualByComparingTo(BigDecimal.valueOf(925_000));
    }

    @Test
    @DisplayName("Gross below standard deduction → taxable floored at zero")
    void grossBelowStandardDeduction_taxableIncomeIsZero() {
        NewRegimeResult result = calculator.calculate(BigDecimal.valueOf(50_000));

        assertThat(result.taxableIncome()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.taxAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
