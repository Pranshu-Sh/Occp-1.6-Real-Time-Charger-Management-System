package com.zyelectric.ocpp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "stop_transaction")
@IdClass(StopTransactionId.class)
public class StopTransaction {
    @Id
    @Column(name = "transaction_id", nullable = false)
    private Integer transactionId;

    @Id
    @Column(name = "event_timestamp", nullable = false)
    private Long eventTimestamp;

    @ManyToOne
    @JoinColumn(name = "connector_id", referencedColumnName = "connector_pk", nullable = false)
    private Connector connector;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", referencedColumnName = "transaction_id", insertable = false, updatable = false)
    private StartTransaction startTransaction;

    @Column(name = "meter_stop")
    private Integer meterStop;

    @Column(name = "stop_timestamp")
    private Long stopTimestamp;

    @Column(name = "reason")
    private String reason;

}
