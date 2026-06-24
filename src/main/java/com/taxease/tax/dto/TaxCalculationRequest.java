package com.taxease.tax.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TaxCalculationRequest {

    @NotNull
    @DecimalMin("0")
    private BigDecimal grossSalary;

    @NotNull
    @DecimalMin("0")
    private BigDecimal basicSalary;

    @NotNull
    @DecimalMin("0")
    private BigDecimal hraReceived;

    @NotNull
    @DecimalMin("0")
    private BigDecimal rentPaid;

    @NotNull
    @DecimalMin("0")
    private BigDecimal investment80C;

    @NotNull
    @DecimalMin("0")
    private BigDecimal medical80D;

    @NotNull
    @DecimalMin("0")
    private BigDecimal homeLoanInterest;

    private boolean metroCity;
}
