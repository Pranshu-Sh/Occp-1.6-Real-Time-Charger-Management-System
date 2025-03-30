package com.zyelectric.ocpp.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "connector")
public class Connector {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "connector_pk")
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "charge_box_id", referencedColumnName = "charge_box_id", nullable = false)
    private ChargeBox chargeBox;

    @Column(name = "connector_id", nullable = false)
    private Integer connectorId;
}
