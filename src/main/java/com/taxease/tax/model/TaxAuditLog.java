package com.taxease.tax.model;

import com.taxease.tax.model.enums.Regime;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tax_audit_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String taxRecordId;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal grossSalary;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Regime recommendedRegime;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal savings;

    @Column(nullable = false)
    private LocalDateTime processedAt;
}
