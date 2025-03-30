package com.zyelectric.ocpp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.Instant;

@Data
@Entity
@Table(name = "connector_status")
public class ConnectorStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "status_id")    // ✅ Unique ID for each status row
    private Long id;

    @ManyToOne
    @JoinColumn(name = "connector_id", referencedColumnName = "connector_pk", nullable = false)
    private Connector connector;

    @Column(name = "status_timestamp")
    private Instant statusTimestamp;

    @Column(name = "status")
    private String status;

    @Column(name = "error_code")
    private String errorCode;

    @Column(name = "info")
    private String Info;

}
