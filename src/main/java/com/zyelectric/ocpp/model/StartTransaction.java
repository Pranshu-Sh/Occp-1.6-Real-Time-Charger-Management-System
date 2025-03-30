package com.zyelectric.ocpp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@Entity
@Table(name = "start_transaction")
public class StartTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id", nullable = false, unique = true)
    private Integer transactionId;

    @ManyToOne
    @JoinColumn(name = "id_tag", referencedColumnName = "id_tag", nullable = false)
    private IdTag idTag;

    @ManyToOne
    @JoinColumn(name = "connector_id", referencedColumnName = "connector_pk", nullable = false)
    private Connector connector;

    @Column(name = "meter_start")
    private Double meterStart;

    @Column(name = "start_timestamp")
    private Long startTimestamp;

}
