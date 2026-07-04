package com.taxease.kafka.event;

import com.taxease.tax.model.TaxStatus;
import com.taxease.tax.model.enums.Regime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaxEvent {

    private String eventId;
    private EventType eventType;
    private String taxRecordId;
    private String userId;
    private Integer taxYear;
    private TaxStatus status;
    private BigDecimal grossSalary;
    private Regime recommendedRegime;
    private BigDecimal savings;
    private LocalDateTime occurredAt;

    public enum EventType {
        TAX_RECORD_CREATED,
        TAX_RECORD_UPDATED,
        TAX_RECORD_SUBMITTED,
        TAX_RECORD_ACCEPTED,
        TAX_RECORD_REJECTED
    }
}
