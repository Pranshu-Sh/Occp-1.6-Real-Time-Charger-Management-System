package com.zyelectric.ocpp.repository;

import com.zyelectric.ocpp.model.MeterValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface MeterValueRepository extends JpaRepository<MeterValue, Long> {

    // ✅ Find meter values by charger ID
    List<MeterValue> findByConnector_ChargeBox_ChargeBoxId(String chargeBoxId);

    // ✅ Find meter values by connector ID
    List<MeterValue> findByConnector_ConnectorId(Integer connectorId);

    // ✅ Find meter values within a time range
    Optional<List<MeterValue>> findByValueTimestampBetween(long startTime, long endTime);

    // ✅ Find meter values by charger ID and time range
    List<MeterValue> findByConnector_ChargeBox_ChargeBoxIdAndValueTimestampBetween(
            String chargeBoxId, long startTime, long endTime);

    @Query("SELECT mv FROM MeterValue mv WHERE mv.connector.id = :connectorId AND mv.valueTimestamp BETWEEN :startTime AND :endTime")
    List<MeterValue> findByConnectorAndTimeRange(@Param("connectorId") Integer connectorId,
                                                 @Param("startTime") Long startTime,
                                                 @Param("endTime") Long endTime);
}
