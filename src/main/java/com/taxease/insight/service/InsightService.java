package com.taxease.insight.service;

import com.taxease.insight.dto.TaxInsightDTO;
import com.taxease.tax.model.TaxRecord;
import com.taxease.tax.model.enums.Regime;
import com.taxease.tax.repository.TaxRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InsightService {

    private final TaxRecordRepository taxRecordRepository;

    @Cacheable(value = "insights", key = "#userId + ':' + #taxYear")
    public TaxInsightDTO generateInsights(String userId, Integer taxYear) {
        TaxRecord record = taxRecordRepository.findByUserId(userId).stream()
                .filter(r -> r.getCalculatedAt().getYear() == taxYear)
                .max(Comparator.comparing(TaxRecord::getCalculatedAt))
                .orElseThrow(() -> new RuntimeException(
                        "No tax record found for user " + userId + " / year " + taxYear));

        BigDecimal effectiveTax = record.getRecommendedRegime() == Regime.OLD
                ? record.getTaxOldRegime() : record.getTaxNewRegime();

        BigDecimal effectiveTaxRate = BigDecimal.ZERO;
        if (record.getGrossSalary().compareTo(BigDecimal.ZERO) > 0) {
            effectiveTaxRate = effectiveTax
                    .divide(record.getGrossSalary(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        return TaxInsightDTO.builder()
                .userId(userId)
                .taxYear(taxYear)
                .effectiveTaxRate(effectiveTaxRate)
                .estimatedRefund(BigDecimal.ZERO)
                .potentialSavings(record.getSavings())
                .recommendations(buildRecommendations(record))
                .build();
    }

    private List<String> buildRecommendations(TaxRecord record) {
        List<String> recs = new ArrayList<>();
        recs.add("Recommended regime: " + record.getRecommendedRegime().name()
                + " — you save ₹" + record.getSavings().toPlainString() + " vs the other regime.");
        if (record.getSavings().compareTo(BigDecimal.ZERO) > 0) {
            recs.add("File under the " + record.getRecommendedRegime().name()
                    + " regime to minimise your tax outflow.");
        }
        recs.add("Maximise Section 80C investments (PPF, ELSS, EPF) up to ₹1,50,000 under the old regime.");
        recs.add("Health insurance premiums qualify under Section 80D up to ₹50,000.");
        recs.add("Home loan interest up to ₹2,00,000 is deductible under Section 24(b) in the old regime.");
        return recs;
    }
}
