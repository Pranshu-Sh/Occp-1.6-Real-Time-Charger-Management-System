package com.zyelectric.ocpp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "charge_box")
public class ChargeBox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "charge_box_pk")
    private int id;

    @Column(name = "charge_box_id", nullable = false, unique = true)
    private String chargeBoxId;

    @Column(name = "registration_status", nullable = false)
    private String registrationStatus;

    @Column(name = "charge_point_vendor")
    private String chargePointVendor;

    @Column(name = "charge_point_model")
    private String chargePointModel;

    @Column(name = "charge_point_serial_number")
    private String chargePointSerialNumber;

    @Column(name = "charge_box_serial_number")
    private String chargeBoxSerialNumber;

    @Column(name = "fw_version")
    private String firmwareVersion;

    @Column(name = "last_heartbeat_timestamp")
    private Long lastHeartbeatTimestamp;

    @Column(name = "status")
    private String status;

}
