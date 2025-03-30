package com.zyelectric.ocpp.repository;

import com.zyelectric.ocpp.model.ChargeBox;
import com.zyelectric.ocpp.model.Connector;
import com.zyelectric.ocpp.model.MeterValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConnectorRepository extends JpaRepository<Connector, Long> {

    Optional<Connector> findByChargeBoxAndConnectorId(ChargeBox chargeBox, Integer connectorId);
    @Query("SELECT c FROM Connector c JOIN c.chargeBox cb WHERE cb.chargeBoxId = :chargerId")
    List<Connector> findByChargerId(@Param("chargerId") String chargerId);

    @Query("SELECT mv FROM MeterValue mv WHERE mv.connector.id = :connectorId " +
            "AND mv.valueTimestamp BETWEEN :startTime AND :endTime")
    List<MeterValue> findByConnectorAndTimeRange(@Param("connectorId") Integer connectorId,
                                                 @Param("startTime") Long startTime,
                                                 @Param("endTime") Long endTime);
}
