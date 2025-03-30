package com.zyelectric.ocpp.service;

import com.zyelectric.ocpp.dto.StatusNotification;
import com.zyelectric.ocpp.model.ChargeBox;
import com.zyelectric.ocpp.model.Connector;

import java.util.Optional;

public interface ConnectorService {

    Optional<Connector> registerConnector(ChargeBox chargeBox, StatusNotification statusNotification);

    Optional<Connector> getConnector(ChargeBox chargeBox, Integer connectorId);
}
