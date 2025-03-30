package com.zyelectric.ocpp.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@EqualsAndHashCode
public class MeterValueId implements Serializable {

    private Integer transactionId;       // FK to transaction_start
    private Long valueTimestamp;   // Unique timestamp for the value
    private String value;
    public MeterValueId() {
    }

    public MeterValueId(Integer transactionId, Long valueTimestamp, String value) {
        this.transactionId = transactionId;
        this.valueTimestamp = valueTimestamp;
        this.value = value;
    }
}
