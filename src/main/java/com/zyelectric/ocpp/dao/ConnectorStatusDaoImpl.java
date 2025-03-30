package com.zyelectric.ocpp.dao;

import com.zyelectric.ocpp.model.ConnectorStatus;
import com.zyelectric.ocpp.repository.ConnectorStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ConnectorStatusDaoImpl implements ConnectorStatusDao {

    private final ConnectorStatusRepository connectorStatusRepository;

    @Override
    public ConnectorStatus save(ConnectorStatus connectorStatus) {
        return connectorStatusRepository.save(connectorStatus);
    }

}
