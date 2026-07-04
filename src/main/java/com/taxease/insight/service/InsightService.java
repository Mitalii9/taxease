package com.taxease.insight.service;

import com.taxease.insight.dto.InsightResponse;
import com.taxease.insight.dto.TaxInsightDTO;
import com.taxease.tax.dto.TaxCalculationRequest;
import com.taxease.tax.dto.TaxCalculationResponse;
import com.taxease.tax.dto.TaxCalculationResponse.DeductionBreakdown;
import com.taxease.tax.model.TaxRecord;
import com.taxease.tax.model.enums.Regime;
import com.taxease.tax.repository.TaxRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InsightService {

    private final GeminiClient geminiClient;
    private final TaxRecordRepository taxRecordRepository;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * Cache-aside: checks Redis before calling Gemini. Redis failures are non-fatal —
     * a warning is logged and the request falls through to Gemini as normal.
     */
    public InsightResponse getTaxSavingTips(TaxCalculationRequest request,
                                            TaxCalculationResponse calc,
                                            String userId) {
        String cacheKey = buildInsightCacheKey(userId, request);

        // ── Cache read ────────────────────────────────────────────────────────
        try {
            String cached = stringRedisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                log.info(">>> REDIS CACHE HIT for key {}", cacheKey);
                return buildInsightResponse(cached, calc);
            }
            log.info(">>> REDIS CACHE MISS - calling Gemini for key {}", cacheKey);
        } catch (Exception e) {
            log.warn("Redis read failed, falling through to Gemini: {}", e.getMessage());
        }

        // ── Gemini call ───────────────────────────────────────────────────────
        String tips;
        try {
            tips = geminiClient.generate(buildPrompt(calc));
        } catch (Exception e) {
            log.warn("Gemini API unavailable, using fallback tips: {}", e.getMessage());
            tips = fallbackTips(calc);
        }

        // ── Cache write (best-effort, 1 h TTL) ───────────────────────────────
        try {
            stringRedisTemplate.opsForValue().set(cacheKey, tips, Duration.ofHours(1));
        } catch (Exception e) {
            log.warn("Redis write failed, response will not be cached: {}", e.getMessage());
        }

        return buildInsightResponse(tips, calc);
    }

    private String buildInsightCacheKey(String userId, TaxCalculationRequest request) {
        return String.format("insights:%s:%s:%s:%s:%s:%s:%s:%s",
                userId,
                request.getGrossSalary().toPlainString(),
                request.getInvestment80C().toPlainString(),
                request.getMedical80D().toPlainString(),
                request.getHomeLoanInterest().toPlainString(),
                request.getHraReceived().toPlainString(),
                request.getRentPaid().toPlainString(),
                request.isMetroCity());
    }

    private InsightResponse buildInsightResponse(String tips, TaxCalculationResponse calc) {
        return InsightResponse.builder()
                .tips(tips)
                .recommendedRegime(calc.getRecommendedRegime())
                .savings(calc.getSavings())
                .taxOldRegime(calc.getTaxOldRegime())
                .taxNewRegime(calc.getTaxNewRegime())
                .build();
    }

    private String buildPrompt(TaxCalculationResponse calc) {
        DeductionBreakdown d = calc.getDeductions();
        return """
                You are a certified Indian tax advisor for FY 2025-26. A salaried employee has the following tax profile:

                Gross Salary: ₹%s

                OLD REGIME:
                  Standard Deduction : ₹%s
                  HRA Exemption      : ₹%s
                  Section 80C        : ₹%s (limit ₹1,50,000)
                  Section 80D        : ₹%s (limit ₹50,000)
                  Home Loan Sec 24(b): ₹%s (limit ₹2,00,000)
                  Total Deductions   : ₹%s
                  Taxable Income     : ₹%s
                  Tax Payable        : ₹%s

                NEW REGIME:
                  Standard Deduction : ₹%s
                  Taxable Income     : ₹%s
                  Tax Payable        : ₹%s

                RECOMMENDED REGIME: %s — saves ₹%s compared to the other regime.

                Give 3-4 personalised, actionable Indian tax-saving tips for this specific user in plain English. \
                Focus on concrete steps they can take to reduce their tax bill further in FY 2025-26. \
                Be specific to their income level and highlight any unused deduction headroom."""
                .formatted(
                        fmt(calc.getGrossSalary()),
                        fmt(d.getStandardDeductionOld()),
                        fmt(d.getHraExemption()),
                        fmt(d.getDeduction80C()),
                        fmt(d.getDeduction80D()),
                        fmt(d.getHomeLoanInterestSec24()),
                        fmt(d.getTotalOldRegimeDeductions()),
                        fmt(d.getTaxableIncomeOldRegime()),
                        fmt(calc.getTaxOldRegime()),
                        fmt(d.getStandardDeductionNew()),
                        fmt(d.getTaxableIncomeNewRegime()),
                        fmt(calc.getTaxNewRegime()),
                        calc.getRecommendedRegime().name(),
                        fmt(calc.getSavings())
                );
    }

    private String fallbackTips(TaxCalculationResponse calc) {
        DeductionBreakdown d = calc.getDeductions();
        List<String> tips = new ArrayList<>();

        tips.add("Switch to the " + calc.getRecommendedRegime().name()
                + " regime to save ₹" + fmt(calc.getSavings()) + " in taxes this year.");

        BigDecimal unused80C = BigDecimal.valueOf(150_000).subtract(d.getDeduction80C());
        if (unused80C.compareTo(BigDecimal.ZERO) > 0) {
            tips.add("You have ₹" + fmt(unused80C)
                    + " of unused Section 80C headroom. Consider investing in PPF, ELSS mutual funds,"
                    + " or increasing your EPF voluntary contribution.");
        }

        BigDecimal unused80D = BigDecimal.valueOf(50_000).subtract(d.getDeduction80D());
        if (unused80D.compareTo(BigDecimal.ZERO) > 0) {
            tips.add("You have ₹" + fmt(unused80D)
                    + " of unused Section 80D headroom. A health insurance policy for yourself"
                    + " or parents can bring this benefit.");
        }

        if (d.getHraExemption().compareTo(BigDecimal.ZERO) == 0) {
            tips.add("If you pay rent, ensure your employer includes HRA in your salary structure."
                    + " The HRA exemption under the old regime can significantly reduce your taxable income.");
        }

        tips.add("Consider NPS contributions under Section 80CCD(1B) for an additional ₹50,000 deduction"
                + " over and above the 80C limit, available under the old regime.");

        return String.join("\n\n", tips);
    }

    private String fmt(BigDecimal value) {
        return value == null ? "0" : value.setScale(0, RoundingMode.HALF_UP).toPlainString();
    }

    // ── Legacy endpoint (history-based insights) ─────────────────────────────

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
                .recommendations(buildLegacyRecommendations(record))
                .build();
    }

    private List<String> buildLegacyRecommendations(TaxRecord record) {
        List<String> recs = new ArrayList<>();
        recs.add("Recommended regime: " + record.getRecommendedRegime().name()
                + " — saves ₹" + record.getSavings().toPlainString() + " vs the other regime.");
        recs.add("Maximise Section 80C investments (PPF, ELSS, EPF) up to ₹1,50,000.");
        recs.add("Health insurance premiums qualify under Section 80D up to ₹50,000.");
        recs.add("Home loan interest up to ₹2,00,000 is deductible under Section 24(b) in the old regime.");
        return recs;
    }
}
