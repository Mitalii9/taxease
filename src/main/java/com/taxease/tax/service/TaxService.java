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

    @Transactional
    public TaxCalculationResponse calculate(TaxCalculationRequest request, String userId) {
        OldRegimeResult oldResult = oldRegimeCalculator.calculate(request);
        NewRegimeResult newResult = newRegimeCalculator.calculate(request.getGrossSalary());

        Regime recommended = oldResult.taxAmount().compareTo(newResult.taxAmount()) <= 0
                ? Regime.OLD : Regime.NEW;

        BigDecimal savings = oldResult.taxAmount().subtract(newResult.taxAmount())
                .abs().setScale(2, RoundingMode.HALF_UP);

        TaxRecord record = TaxRecord.builder()
                .userId(userId)
                .grossSalary(request.getGrossSalary())
                .regimeChosen(recommended)
                .taxOldRegime(oldResult.taxAmount())
                .taxNewRegime(newResult.taxAmount())
                .recommendedRegime(recommended)
                .savings(savings)
                .build();
        taxRecordRepository.save(record);

        TaxCalculationResponse.DeductionBreakdown breakdown = TaxCalculationResponse.DeductionBreakdown.builder()
                .standardDeductionOld(oldResult.standardDeduction())
                .hraExemption(oldResult.hraExemption())
                .deduction80C(oldResult.deduction80C())
                .deduction80D(oldResult.deduction80D())
                .homeLoanInterestSec24(oldResult.homeLoanInterest())
                .totalOldRegimeDeductions(oldResult.totalDeductions())
                .taxableIncomeOldRegime(oldResult.taxableIncome())
                .standardDeductionNew(newResult.standardDeduction())
                .taxableIncomeNewRegime(newResult.taxableIncome())
                .build();

        return TaxCalculationResponse.builder()
                .taxOldRegime(oldResult.taxAmount())
                .taxNewRegime(newResult.taxAmount())
                .recommendedRegime(recommended)
                .savings(savings)
                .deductions(breakdown)
                .build();
    }

    public List<TaxRecordDTO> getHistory(String userId) {
        return taxRecordRepository.findByUserId(userId).stream()
                .map(this::toDTO)
                .toList();
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
