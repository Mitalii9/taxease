package com.taxease.tax.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateTaxRecordRequest {

    @NotBlank
    private String userId;

    @NotNull
    @Min(2000) @Max(2100)
    private Integer taxYear;

    @NotNull
    private BigDecimal grossIncome;

    private BigDecimal taxPaid;
}
