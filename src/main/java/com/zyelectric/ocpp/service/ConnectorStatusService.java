package com.zyelectric.ocpp.service;

import com.zyelectric.ocpp.dto.StatusNotification;
import com.zyelectric.ocpp.model.ChargeBox;
import com.zyelectric.ocpp.model.Connector;
import com.zyelectric.ocpp.model.ConnectorStatus;

import java.util.List;

public interface ConnectorStatusService {

    void saveStatus(Connector connector, StatusNotification statusNotification);

    List<ConnectorStatus> getConnectorStatusHistory(Long connectorId);

    List<ConnectorStatus> getConnectorsForCharger(ChargeBox chargeBox);
}
