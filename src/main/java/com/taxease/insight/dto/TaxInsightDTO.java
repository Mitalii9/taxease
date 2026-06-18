package com.taxease.insight.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class TaxInsightDTO {
    private String userId;
    private Integer taxYear;
    private BigDecimal effectiveTaxRate;
    private BigDecimal estimatedRefund;
    private BigDecimal potentialSavings;
    private List<String> recommendations;
}
