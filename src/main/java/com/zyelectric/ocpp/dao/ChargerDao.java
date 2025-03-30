package com.zyelectric.ocpp.dao;

import com.zyelectric.ocpp.model.ChargeBox;

import java.util.List;
import java.util.Optional;

public interface ChargerDao {

    ChargeBox save(ChargeBox chargeBox);

    List<ChargeBox> findAll();

    Optional<ChargeBox> findByChargeBoxId(String chargeBoxId);

    void updateHeartbeat(String chargeBoxId);
}
