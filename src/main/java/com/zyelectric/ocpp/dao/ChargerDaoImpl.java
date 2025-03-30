package com.zyelectric.ocpp.dao;

import com.zyelectric.ocpp.model.ChargeBox;
import com.zyelectric.ocpp.repository.ChargeBoxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ChargerDaoImpl implements ChargerDao {

    private final ChargeBoxRepository chargeBoxRepository;

    @Override
    public ChargeBox save(ChargeBox chargeBox) {
        return chargeBoxRepository.save(chargeBox);
    }

    @Override
    public List<ChargeBox> findAll() {
        return chargeBoxRepository.findAll();
    }

    @Override
    public Optional<ChargeBox> findByChargeBoxId(String chargeBoxId) {
        return chargeBoxRepository.findByChargeBoxId(chargeBoxId);
    }

    @Override
    public void updateHeartbeat(String chargeBoxId) {
        chargeBoxRepository.findByChargeBoxId(chargeBoxId)
                .ifPresent(charger -> {
                    charger.setLastHeartbeatTimestamp(Instant.now().toEpochMilli());
                    chargeBoxRepository.save(charger);
                });
    }
}
