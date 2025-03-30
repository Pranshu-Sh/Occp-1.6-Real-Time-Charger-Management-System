package com.zyelectric.ocpp.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;

@Getter
@Setter
@EqualsAndHashCode
public class StopTransactionId implements Serializable {

    private Integer transactionId;
    private Long eventTimestamp;

    public StopTransactionId() {
    }

    public StopTransactionId(Integer transactionId, Long eventTimestamp) {
        this.transactionId = transactionId;
        this.eventTimestamp = eventTimestamp;
    }
}
