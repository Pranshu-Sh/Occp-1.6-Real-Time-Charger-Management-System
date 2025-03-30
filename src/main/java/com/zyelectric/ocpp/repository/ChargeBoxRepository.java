package com.zyelectric.ocpp.repository;

import com.zyelectric.ocpp.model.ChargeBox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ChargeBoxRepository extends JpaRepository<ChargeBox, Long> {

    Optional<ChargeBox> findByChargeBoxId(String chargeBoxId);

    Optional<ChargeBox> findByChargeBoxSerialNumber(String serialNumber);

    List<ChargeBox> findByLastHeartbeatTimestampBefore(long timestamp);

    @Query("""
                SELECT cb.chargeBoxId, cb.status
                FROM ChargeBox cb
            """)
    List<ChargeBox> findAllChargersWithStatus();
}
