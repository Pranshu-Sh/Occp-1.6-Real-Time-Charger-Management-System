package com.zyelectric.ocpp.service;

import com.zyelectric.ocpp.dto.StatusNotification;
import com.zyelectric.ocpp.model.ChargeBox;
import com.zyelectric.ocpp.model.Connector;
import com.zyelectric.ocpp.model.ConnectorStatus;
import com.zyelectric.ocpp.repository.ConnectorStatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConnectorStatusServiceImpl implements ConnectorStatusService {

    private final ConnectorStatusRepository connectorStatusRepository;

    @Override
    public void saveStatus(Connector connector, StatusNotification statusNotification) {
        ConnectorStatus status = new ConnectorStatus();
        status.setConnector(connector);
        status.setStatus(statusNotification.getStatus());
        status.setErrorCode(statusNotification.getErrorCode());
        status.setInfo(statusNotification.getInfo());
        status.setStatusTimestamp(statusNotification.getStatusTimestamp());

        connectorStatusRepository.save(status);

        log.info("Saved connector status for Connector ID: {} with status: {}",
                connector.getConnectorId(), statusNotification.getStatus());
    }

    @Override
    public List<ConnectorStatus> getConnectorStatusHistory(Long connectorId) {
        return connectorStatusRepository.findByConnectorId(connectorId);
    }
    @Override
    public List<ConnectorStatus> getConnectorsForCharger(ChargeBox chargeBox) {
        return connectorStatusRepository.findConnectorsByChargeBox(chargeBox);
    }

}
