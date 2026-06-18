package com.taxease.insight.service;

import com.taxease.insight.dto.TaxInsightDTO;
import com.taxease.tax.model.TaxRecord;
import com.taxease.tax.repository.TaxRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InsightService {

    private final TaxRecordRepository taxRecordRepository;

    @Cacheable(value = "insights", key = "#userId + ':' + #taxYear")
    public TaxInsightDTO generateInsights(String userId, Integer taxYear) {
        TaxRecord record = taxRecordRepository.findByUserIdAndTaxYear(userId, taxYear)
                .orElseThrow(() -> new RuntimeException("No tax record found for user " + userId + " / year " + taxYear));

        BigDecimal effectiveTaxRate = BigDecimal.ZERO;
        if (record.getGrossIncome() != null && record.getGrossIncome().compareTo(BigDecimal.ZERO) > 0) {
            effectiveTaxRate = record.getTaxLiability()
                    .divide(record.getGrossIncome(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        List<String> recommendations = buildRecommendations(record);

        return TaxInsightDTO.builder()
                .userId(userId)
                .taxYear(taxYear)
                .effectiveTaxRate(effectiveTaxRate)
                .estimatedRefund(record.getRefundOrDue())
                .potentialSavings(estimatePotentialSavings(record))
                .recommendations(recommendations)
                .build();
    }

    private BigDecimal estimatePotentialSavings(TaxRecord record) {
        // placeholder — real logic would analyze deductions, retirement contributions, etc.
        return record.getTaxLiability().multiply(BigDecimal.valueOf(0.05)).setScale(2, RoundingMode.HALF_UP);
    }

    private List<String> buildRecommendations(TaxRecord record) {
        List<String> recs = new ArrayList<>();
        if (record.getRefundOrDue() != null && record.getRefundOrDue().compareTo(BigDecimal.ZERO) < 0) {
            recs.add("You have an outstanding tax liability. Consider adjusting your withholding.");
        }
        recs.add("Review available deductions to reduce your taxable income.");
        recs.add("Consider contributing to a tax-advantaged retirement account.");
        return recs;
    }
}
