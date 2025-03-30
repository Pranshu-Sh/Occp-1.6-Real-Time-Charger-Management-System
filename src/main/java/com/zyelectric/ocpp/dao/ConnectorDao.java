package com.zyelectric.ocpp.dao;

import com.zyelectric.ocpp.model.ChargeBox;
import com.zyelectric.ocpp.model.Connector;

import java.util.List;
import java.util.Optional;

public interface ConnectorDao {

    Connector save(Connector connector);

    Optional<Connector> findByChargeBoxAndConnectorId(ChargeBox chargeBox, Integer connectorId);
}
