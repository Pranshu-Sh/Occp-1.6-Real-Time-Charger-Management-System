package com.zyelectric.ocpp.service;

import com.zyelectric.ocpp.dto.StatusNotification;
import com.zyelectric.ocpp.model.ChargeBox;
import com.zyelectric.ocpp.model.Connector;
import com.zyelectric.ocpp.repository.ConnectorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConnectorServiceImpl implements ConnectorService {

    private final ConnectorRepository connectorRepository;

    @Override
    public Optional<Connector> registerConnector(ChargeBox chargeBox, StatusNotification statusNotification) {
        Optional<Connector> existingConnector = getConnector(chargeBox, statusNotification.getConnectorId());

        if (existingConnector.isPresent()) {
            log.info("Connector already exists: {}", statusNotification.getConnectorId());
            return existingConnector;
        }

        Connector newConnector = new Connector();
        newConnector.setConnectorId(statusNotification.getConnectorId());
        newConnector.setChargeBox(chargeBox);

        connectorRepository.save(newConnector);
        log.info("Registered new connector: {}", statusNotification.getConnectorId());

        return Optional.of(newConnector);
    }

    @Override
    public Optional<Connector> getConnector(ChargeBox chargeBox, Integer connectorId) {
        return connectorRepository.findByChargeBoxAndConnectorId(chargeBox, connectorId);
    }
}
