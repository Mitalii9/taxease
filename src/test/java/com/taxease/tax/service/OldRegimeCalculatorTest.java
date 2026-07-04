package com.taxease.tax.service;

import com.taxease.tax.dto.TaxCalculationRequest;
import com.taxease.tax.service.OldRegimeCalculator.OldRegimeResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class OldRegimeCalculatorTest {

    private final OldRegimeCalculator calculator = new OldRegimeCalculator();

    // ── HRA exemption ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("HRA exemption = actualHra when it is the smallest of the three components")
    void hraExemption_actualHraIsMinimum() {
        // actualHra      = 1,00,000
        // rentMinus10pct = 3,00,000 − (5,00,000 × 10%) = 2,50,000
        // locationBased  = 5,00,000 × 50% (metro)      = 2,50,000
        // min → 1,00,000
        TaxCalculationRequest req = request(
                1_000_000, 500_000, 100_000, 300_000, true,
                0, 0, 0);

        OldRegimeResult result = calculator.calculate(req);

        assertThat(result.hraExemption()).isEqualByComparingTo(BigDecimal.valueOf(100_000));
    }

    @Test
    @DisplayName("HRA exemption = rent − 10% basic when that is the smallest component")
    void hraExemption_rentMinusTenPercentIsMinimum() {
        // actualHra      = 3,00,000
        // rentMinus10pct = 1,20,000 − (5,00,000 × 10%) = 70,000
        // locationBased  = 5,00,000 × 50% (metro)      = 2,50,000
        // min → 70,000
        TaxCalculationRequest req = request(
                1_000_000, 500_000, 300_000, 120_000, true,
                0, 0, 0);

        OldRegimeResult result = calculator.calculate(req);

        assertThat(result.hraExemption()).isEqualByComparingTo(BigDecimal.valueOf(70_000));
    }

    @Test
    @DisplayName("HRA exemption = 40% of basic (non-metro) when that is the smallest component")
    void hraExemption_locationBasedIsMinimum() {
        // actualHra      = 3,00,000
        // rentMinus10pct = 3,00,000 − (2,00,000 × 10%) = 2,80,000
        // locationBased  = 2,00,000 × 40% (non-metro)  = 80,000
        // min → 80,000
        TaxCalculationRequest req = request(
                1_000_000, 200_000, 300_000, 300_000, false,
                0, 0, 0);

        OldRegimeResult result = calculator.calculate(req);

        assertThat(result.hraExemption()).isEqualByComparingTo(BigDecimal.valueOf(80_000));
    }

    @Test
    @DisplayName("HRA exemption is zero when no rent is paid")
    void hraExemption_noRentPaid_returnsZero() {
        TaxCalculationRequest req = request(
                1_000_000, 500_000, 200_000, 0, true,
                0, 0, 0);

        OldRegimeResult result = calculator.calculate(req);

        assertThat(result.hraExemption()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ── Deduction caps ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("80C capped at 1,50,000 when investment exceeds the limit")
    void deduction80C_cappedAtOneFiftyThousand() {
        TaxCalculationRequest req = request(
                1_500_000, 600_000, 0, 0, false,
                200_000, 0, 0);

        OldRegimeResult result = calculator.calculate(req);

        assertThat(result.deduction80C()).isEqualByComparingTo(BigDecimal.valueOf(150_000));
    }

    @Test
    @DisplayName("80D capped at 50,000 when medical premium exceeds the limit")
    void deduction80D_cappedAtFiftyThousand() {
        TaxCalculationRequest req = request(
                1_500_000, 600_000, 0, 0, false,
                0, 75_000, 0);

        OldRegimeResult result = calculator.calculate(req);

        assertThat(result.deduction80D()).isEqualByComparingTo(BigDecimal.valueOf(50_000));
    }

    @Test
    @DisplayName("Home loan interest capped at 2,00,000 under Sec 24(b)")
    void homeLoanInterest_cappedAtTwoLakh() {
        TaxCalculationRequest req = request(
                1_500_000, 600_000, 0, 0, false,
                0, 0, 300_000);

        OldRegimeResult result = calculator.calculate(req);

        assertThat(result.homeLoanInterest()).isEqualByComparingTo(BigDecimal.valueOf(200_000));
    }

    // ── Full-scenario tax calculation ──────────────────────────────────────────

    @Test
    @DisplayName("Gross 12L with full deductions → tax 20,280 (slabs + 4% cess)")
    void grossTwelveLakh_fullDeductions_returnsTwentyThousandTwoEighty() {
        // HRA: min(2,15,000 | 3,00,000−50,000=2,50,000 | 50%×5,00,000=2,50,000) = 2,15,000
        // Deductions: 50k + 2,15k + 1,50k + 50k + 2,00k = 6,65,000
        // Taxable   : 12,00,000 − 6,65,000 = 5,35,000  (> 5L → no rebate)
        // 5%  on 2,50,000 = 12,500
        // 20% on    35,000 =  7,000
        // raw = 19,500  →  cess 780  →  total 20,280
        TaxCalculationRequest req = request(
                1_200_000, 500_000, 215_000, 300_000, true,
                150_000, 50_000, 200_000);

        OldRegimeResult result = calculator.calculate(req);

        assertThat(result.taxAmount()).isEqualByComparingTo(BigDecimal.valueOf(20_280));
    }

    // ── Rebate 87A ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("87A rebate applies when taxable income is exactly 5L → zero tax")
    void rebate87A_taxableIncomeAtFiveLakh_returnsZeroTax() {
        // gross 5,50,000 − standard 50,000 = 5,00,000 taxable ≤ 5L rebate limit
        TaxCalculationRequest req = request(
                550_000, 200_000, 0, 0, false,
                0, 0, 0);

        OldRegimeResult result = calculator.calculate(req);

        assertThat(result.taxAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("87A rebate does NOT apply when taxable income exceeds 5L")
    void rebate87A_taxableAboveFiveLakh_taxIsPositive() {
        // gross 6,00,000 − standard 50,000 = 5,50,000 > rebate limit
        TaxCalculationRequest req = request(
                600_000, 200_000, 0, 0, false,
                0, 0, 0);

        OldRegimeResult result = calculator.calculate(req);

        assertThat(result.taxAmount().compareTo(BigDecimal.ZERO)).isPositive();
    }

    // ── Helper ─────────────────────────────────────────────────────────────────

    private static TaxCalculationRequest request(
            long gross, long basic, long hra, long rent, boolean metro,
            long c80, long d80, long homeLoan) {
        TaxCalculationRequest r = new TaxCalculationRequest();
        r.setGrossSalary(BigDecimal.valueOf(gross));
        r.setBasicSalary(BigDecimal.valueOf(basic));
        r.setHraReceived(BigDecimal.valueOf(hra));
        r.setRentPaid(BigDecimal.valueOf(rent));
        r.setMetroCity(metro);
        r.setInvestment80C(BigDecimal.valueOf(c80));
        r.setMedical80D(BigDecimal.valueOf(d80));
        r.setHomeLoanInterest(BigDecimal.valueOf(homeLoan));
        return r;
    }
}
