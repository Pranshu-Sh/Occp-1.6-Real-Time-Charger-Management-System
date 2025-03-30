package com.zyelectric.ocpp.service;

import com.zyelectric.ocpp.dto.BootNotification;
import com.zyelectric.ocpp.model.ChargeBox;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ChargerService {

    Optional<ChargeBox> registerCharger(String chargePointName, BootNotification bootNotification);

    List<ChargeBox> getAllChargers();

    Optional<ChargeBox> getChargerById(String chargeBoxId);

    void updateHeartbeat(String chargeBoxId, long timeStamp);

    void updateStatus(String chargeBoxId, String status);

    void preRegisterCharger(String charger);

    Optional<ChargeBox> getChargerBySerialNumber(String serialNumber);

}
