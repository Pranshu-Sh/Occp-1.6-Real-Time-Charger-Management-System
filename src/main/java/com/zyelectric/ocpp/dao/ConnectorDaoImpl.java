package com.zyelectric.ocpp.dao;

import com.zyelectric.ocpp.model.ChargeBox;
import com.zyelectric.ocpp.model.Connector;
import com.zyelectric.ocpp.repository.ConnectorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ConnectorDaoImpl implements ConnectorDao {

    private final ConnectorRepository connectorRepository;

    @Override
    public Connector save(Connector connector) {
        return connectorRepository.save(connector);
    }
    @Override
    public Optional<Connector> findByChargeBoxAndConnectorId(ChargeBox chargeBox, Integer connectorId) {
        return connectorRepository.findByChargeBoxAndConnectorId(chargeBox, connectorId);
    }
}
