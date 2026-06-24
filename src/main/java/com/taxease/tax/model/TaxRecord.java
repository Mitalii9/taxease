package com.taxease.tax.model;

import com.taxease.tax.model.enums.Regime;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tax_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal grossSalary;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Regime regimeChosen;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal taxOldRegime;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal taxNewRegime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Regime recommendedRegime;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal savings;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime calculatedAt;

    @PrePersist
    void onCreate() {
        calculatedAt = LocalDateTime.now();
    }
}
