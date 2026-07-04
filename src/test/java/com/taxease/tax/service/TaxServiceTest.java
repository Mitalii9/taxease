package com.taxease.tax.service;

import com.taxease.kafka.event.TaxEvent;
import com.taxease.kafka.event.TaxEvent.EventType;
import com.taxease.kafka.producer.TaxEventProducer;
import com.taxease.tax.dto.TaxCalculationRequest;
import com.taxease.tax.dto.TaxCalculationResponse;
import com.taxease.tax.model.TaxRecord;
import com.taxease.tax.model.enums.Regime;
import com.taxease.tax.repository.TaxRecordRepository;
import com.taxease.tax.service.NewRegimeCalculator.NewRegimeResult;
import com.taxease.tax.service.OldRegimeCalculator.OldRegimeResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaxServiceTest {

    @Mock private OldRegimeCalculator oldRegimeCalculator;
    @Mock private NewRegimeCalculator newRegimeCalculator;
    @Mock private TaxRecordRepository taxRecordRepository;
    @Mock private TaxEventProducer taxEventProducer;

    private TaxService taxService;

    // Shared stubs — overridden per test when needed
    private static final BigDecimal GROSS     = BigDecimal.valueOf(1_000_000);
    private static final BigDecimal OLD_TAX   = BigDecimal.valueOf(80_000);
    private static final BigDecimal NEW_TAX   = BigDecimal.valueOf(60_000);
    private static final BigDecimal ZERO      = BigDecimal.ZERO;

    @BeforeEach
    void setUp() {
        taxService = new TaxService(
                oldRegimeCalculator, newRegimeCalculator,
                taxRecordRepository, taxEventProducer);

        // simulate Hibernate assigning a UUID on persist
        doAnswer(inv -> {
            TaxRecord r = inv.getArgument(0);
            r.setId("test-record-id");
            return r;
        }).when(taxRecordRepository).save(any(TaxRecord.class));
    }

    // ── Regime recommendation ──────────────────────────────────────────────────

    @Test
    @DisplayName("OLD regime recommended when old tax ≤ new tax")
    void calculate_oldTaxLower_recommendsOldRegime() {
        // oldTax 60,000 < newTax 80,000  →  OLD wins
        stubCalculators(BigDecimal.valueOf(60_000), BigDecimal.valueOf(80_000));
        TaxCalculationRequest request = buildRequest(GROSS);

        TaxCalculationResponse response = taxService.calculate(request, "user1");

        assertThat(response.getRecommendedRegime()).isEqualTo(Regime.OLD);
    }

    @Test
    @DisplayName("NEW regime recommended when new tax is strictly lower")
    void calculate_newTaxLower_recommendsNewRegime() {
        // oldTax 80,000 > newTax 60,000  →  NEW wins
        stubCalculators(OLD_TAX, NEW_TAX);
        TaxCalculationRequest request = buildRequest(GROSS);

        TaxCalculationResponse response = taxService.calculate(request, "user1");

        assertThat(response.getRecommendedRegime()).isEqualTo(Regime.NEW);
    }

    // ── Savings ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Savings equals absolute difference between the two regime taxes")
    void calculate_savings_equalsAbsoluteDifference() {
        // |80,000 − 60,000| = 20,000
        stubCalculators(OLD_TAX, NEW_TAX);
        TaxCalculationRequest request = buildRequest(GROSS);

        TaxCalculationResponse response = taxService.calculate(request, "user1");

        assertThat(response.getSavings()).isEqualByComparingTo(BigDecimal.valueOf(20_000));
    }

    @Test
    @DisplayName("Savings is always non-negative even when taxes are equal")
    void calculate_savings_isNonNegativeWhenTaxesAreEqual() {
        stubCalculators(BigDecimal.valueOf(50_000), BigDecimal.valueOf(50_000));
        TaxCalculationRequest request = buildRequest(GROSS);

        TaxCalculationResponse response = taxService.calculate(request, "user1");

        assertThat(response.getSavings().compareTo(BigDecimal.ZERO)).isGreaterThanOrEqualTo(0);
    }

    // ── Repository interaction ─────────────────────────────────────────────────

    @Test
    @DisplayName("calculate() saves a TaxRecord to the repository")
    void calculate_savesRecordToRepository() {
        stubCalculators(OLD_TAX, NEW_TAX);
        TaxCalculationRequest request = buildRequest(GROSS);

        taxService.calculate(request, "user1");

        ArgumentCaptor<TaxRecord> captor = ArgumentCaptor.forClass(TaxRecord.class);
        verify(taxRecordRepository).save(captor.capture());

        TaxRecord saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo("user1");
        assertThat(saved.getGrossSalary()).isEqualByComparingTo(GROSS);
        assertThat(saved.getRecommendedRegime()).isEqualTo(Regime.NEW);
    }

    // ── Kafka event ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("calculate() publishes a TAX_RECORD_CREATED event via TaxEventProducer")
    void calculate_publishesTaxCreatedEvent() {
        stubCalculators(OLD_TAX, NEW_TAX);
        TaxCalculationRequest request = buildRequest(GROSS);

        taxService.calculate(request, "user42");

        ArgumentCaptor<TaxEvent> captor = ArgumentCaptor.forClass(TaxEvent.class);
        verify(taxEventProducer).publish(captor.capture());

        TaxEvent event = captor.getValue();
        assertThat(event.getEventType()).isEqualTo(EventType.TAX_RECORD_CREATED);
        assertThat(event.getUserId()).isEqualTo("user42");
        assertThat(event.getTaxRecordId()).isEqualTo("test-record-id");
        assertThat(event.getGrossSalary()).isEqualByComparingTo(GROSS);
        assertThat(event.getRecommendedRegime()).isEqualTo(Regime.NEW);
        assertThat(event.getSavings()).isEqualByComparingTo(BigDecimal.valueOf(20_000));
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getOccurredAt()).isNotNull();
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private void stubCalculators(BigDecimal oldTax, BigDecimal newTax) {
        OldRegimeResult oldResult = new OldRegimeResult(
                oldTax,
                BigDecimal.valueOf(50_000),   // standardDeduction
                BigDecimal.valueOf(100_000),  // hraExemption
                BigDecimal.valueOf(150_000),  // deduction80C
                BigDecimal.valueOf(50_000),   // deduction80D
                BigDecimal.valueOf(200_000),  // homeLoanInterest
                BigDecimal.valueOf(550_000),  // totalDeductions
                BigDecimal.valueOf(450_000)   // taxableIncome
        );
        NewRegimeResult newResult = new NewRegimeResult(
                newTax,
                BigDecimal.valueOf(75_000),   // standardDeduction
                BigDecimal.valueOf(925_000)   // taxableIncome
        );

        when(oldRegimeCalculator.calculate(any())).thenReturn(oldResult);
        when(newRegimeCalculator.calculate(any())).thenReturn(newResult);
    }

    private static TaxCalculationRequest buildRequest(BigDecimal gross) {
        TaxCalculationRequest r = new TaxCalculationRequest();
        r.setGrossSalary(gross);
        r.setBasicSalary(BigDecimal.valueOf(400_000));
        r.setHraReceived(BigDecimal.valueOf(100_000));
        r.setRentPaid(BigDecimal.valueOf(150_000));
        r.setMetroCity(true);
        r.setInvestment80C(BigDecimal.valueOf(150_000));
        r.setMedical80D(BigDecimal.valueOf(50_000));
        r.setHomeLoanInterest(BigDecimal.valueOf(200_000));
        return r;
    }
}
