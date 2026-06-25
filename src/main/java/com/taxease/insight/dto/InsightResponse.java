package com.taxease.insight.dto;

import com.taxease.tax.model.enums.Regime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InsightResponse {

    private String tips;
    private Regime recommendedRegime;
    private BigDecimal savings;
    private BigDecimal taxOldRegime;
    private BigDecimal taxNewRegime;
}
