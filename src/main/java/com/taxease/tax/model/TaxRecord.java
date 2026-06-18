package com.taxease.tax.model;

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

    @Column(nullable = false)
    private Integer taxYear;

    @Column(precision = 15, scale = 2)
    private BigDecimal grossIncome;

    @Column(precision = 15, scale = 2)
    private BigDecimal taxableIncome;

    @Column(precision = 15, scale = 2)
    private BigDecimal taxLiability;

    @Column(precision = 15, scale = 2)
    private BigDecimal taxPaid;

    @Column(precision = 15, scale = 2)
    private BigDecimal refundOrDue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaxStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = TaxStatus.DRAFT;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
