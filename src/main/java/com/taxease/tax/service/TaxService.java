package com.taxease.tax.service;

import com.taxease.tax.dto.TaxCalculationRequest;
import com.taxease.tax.dto.TaxCalculationResponse;
import com.taxease.tax.dto.TaxRecordDTO;
import com.taxease.tax.model.TaxRecord;
import com.taxease.tax.model.enums.Regime;
import com.taxease.tax.repository.TaxRecordRepository;
import com.taxease.tax.service.NewRegimeCalculator.NewRegimeResult;
import com.taxease.tax.service.OldRegimeCalculator.OldRegimeResult;
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

    private final OldRegimeCalculator oldRegimeCalculator;
    private final NewRegimeCalculator newRegimeCalculator;
    private final TaxRecordRepository taxRecordRepository;

    /** Calculates both regimes, persists a TaxRecord, and returns the full response. */
    @Transactional
    public TaxCalculationResponse calculate(TaxCalculationRequest request, String userId) {
        OldRegimeResult oldResult = oldRegimeCalculator.calculate(request);
        NewRegimeResult newResult = newRegimeCalculator.calculate(request.getGrossSalary());
        TaxCalculationResponse response = buildResponse(request.getGrossSalary(), oldResult, newResult);

        TaxRecord record = TaxRecord.builder()
                .userId(userId)
                .grossSalary(request.getGrossSalary())
                .regimeChosen(response.getRecommendedRegime())
                .taxOldRegime(response.getTaxOldRegime())
                .taxNewRegime(response.getTaxNewRegime())
                .recommendedRegime(response.getRecommendedRegime())
                .savings(response.getSavings())
                .build();
        taxRecordRepository.save(record);

        return response;
    }

    /** Calculates both regimes without persisting — used by the Insight endpoint. */
    public TaxCalculationResponse calculateOnly(TaxCalculationRequest request) {
        OldRegimeResult oldResult = oldRegimeCalculator.calculate(request);
        NewRegimeResult newResult = newRegimeCalculator.calculate(request.getGrossSalary());
        return buildResponse(request.getGrossSalary(), oldResult, newResult);
    }

    public List<TaxRecordDTO> getHistory(String userId) {
        return taxRecordRepository.findByUserId(userId).stream()
                .map(this::toDTO)
                .toList();
    }

    private TaxCalculationResponse buildResponse(BigDecimal grossSalary,
                                                 OldRegimeResult old,
                                                 NewRegimeResult newR) {
        Regime recommended = old.taxAmount().compareTo(newR.taxAmount()) <= 0
                ? Regime.OLD : Regime.NEW;
        BigDecimal savings = old.taxAmount().subtract(newR.taxAmount())
                .abs().setScale(2, RoundingMode.HALF_UP);

        TaxCalculationResponse.DeductionBreakdown breakdown = TaxCalculationResponse.DeductionBreakdown.builder()
                .standardDeductionOld(old.standardDeduction())
                .hraExemption(old.hraExemption())
                .deduction80C(old.deduction80C())
                .deduction80D(old.deduction80D())
                .homeLoanInterestSec24(old.homeLoanInterest())
                .totalOldRegimeDeductions(old.totalDeductions())
                .taxableIncomeOldRegime(old.taxableIncome())
                .standardDeductionNew(newR.standardDeduction())
                .taxableIncomeNewRegime(newR.taxableIncome())
                .build();

        return TaxCalculationResponse.builder()
                .grossSalary(grossSalary)
                .taxOldRegime(old.taxAmount())
                .taxNewRegime(newR.taxAmount())
                .recommendedRegime(recommended)
                .savings(savings)
                .deductions(breakdown)
                .build();
    }

    private TaxRecordDTO toDTO(TaxRecord r) {
        return TaxRecordDTO.builder()
                .id(r.getId())
                .userId(r.getUserId())
                .grossSalary(r.getGrossSalary())
                .regimeChosen(r.getRegimeChosen())
                .taxOldRegime(r.getTaxOldRegime())
                .taxNewRegime(r.getTaxNewRegime())
                .recommendedRegime(r.getRecommendedRegime())
                .savings(r.getSavings())
                .calculatedAt(r.getCalculatedAt())
                .build();
    }
}
