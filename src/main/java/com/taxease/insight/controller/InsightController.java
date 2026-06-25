package com.taxease.insight.controller;

import com.taxease.insight.dto.InsightResponse;
import com.taxease.insight.dto.TaxInsightDTO;
import com.taxease.insight.service.InsightService;
import com.taxease.tax.dto.TaxCalculationRequest;
import com.taxease.tax.dto.TaxCalculationResponse;
import com.taxease.tax.service.TaxService;
import com.taxease.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/insights")
@RequiredArgsConstructor
@Tag(name = "Insights", description = "AI-powered tax insights and recommendations")
public class InsightController {

    private final InsightService insightService;
    private final TaxService taxService;
    private final UserService userService;

    @PostMapping("/tax-tips")
    @Operation(summary = "Get AI-generated personalised tax-saving tips for FY 2025-26")
    public ResponseEntity<InsightResponse> getTaxTips(
            @Valid @RequestBody TaxCalculationRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        userService.getProfile(email); // validates the token maps to a real user

        TaxCalculationResponse calc = taxService.calculateOnly(request);
        return ResponseEntity.ok(insightService.getTaxSavingTips(calc));
    }

    @GetMapping("/{userId}/{taxYear}")
    @Operation(summary = "Get stored tax insights for a user and tax year")
    public ResponseEntity<TaxInsightDTO> getInsights(
            @PathVariable String userId,
            @PathVariable Integer taxYear) {
        return ResponseEntity.ok(insightService.generateInsights(userId, taxYear));
    }
}
