package com.taxease.insight.controller;

import com.taxease.insight.dto.TaxInsightDTO;
import com.taxease.insight.service.InsightService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/insights")
@RequiredArgsConstructor
@Tag(name = "Insights", description = "Tax insights and recommendations")
public class InsightController {

    private final InsightService insightService;

    @GetMapping("/{userId}/{taxYear}")
    @Operation(summary = "Generate tax insights for a user and tax year")
    public ResponseEntity<TaxInsightDTO> getInsights(
            @PathVariable String userId,
            @PathVariable Integer taxYear) {
        return ResponseEntity.ok(insightService.generateInsights(userId, taxYear));
    }
}
