package com.zyelectric.ocpp.repository;

import com.zyelectric.ocpp.model.ChargeBox;
import com.zyelectric.ocpp.model.Connector;
import com.zyelectric.ocpp.model.ConnectorStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ConnectorStatusRepository extends JpaRepository<ConnectorStatus, Long> {

    List<ConnectorStatus> findByConnectorId(Long connectorId);
    @Query("SELECT cs FROM ConnectorStatus cs WHERE cs.connector.chargeBox = :chargeBox")
    List<ConnectorStatus> findConnectorsByChargeBox(@Param("chargeBox") ChargeBox chargeBox);
}
