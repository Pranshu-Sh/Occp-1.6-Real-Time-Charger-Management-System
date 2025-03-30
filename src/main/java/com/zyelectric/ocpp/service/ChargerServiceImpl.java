package com.zyelectric.ocpp.service;

import com.zyelectric.ocpp.dto.BootNotification;
import com.zyelectric.ocpp.model.ChargeBox;
import com.zyelectric.ocpp.repository.ChargeBoxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChargerServiceImpl implements ChargerService {

    private final ChargeBoxRepository chargeBoxRepository;

    @Override
    public Optional<ChargeBox> registerCharger(String chargePointName, BootNotification dto) {
        try {
            log.info("chargePointName: {}", chargePointName);
            Optional<ChargeBox> chargerOpt = chargeBoxRepository.findByChargeBoxId(chargePointName);
            ChargeBox charger = chargerOpt.get();
            charger.setChargePointVendor(dto.getChargePointVendor());
            charger.setChargePointModel(dto.getChargePointModel());
            charger.setChargePointSerialNumber(dto.getChargePointSerialNumber());
            charger.setChargeBoxSerialNumber(dto.getChargeBoxSerialNumber());
            charger.setFirmwareVersion(dto.getFirmwareVersion());
            charger.setRegistrationStatus("Accepted");
            charger.setStatus("Available");
            chargeBoxRepository.save(charger);

        } catch (Exception e) {
            log.error("Failed to process register charger: {}", e.getMessage(), e);
        }
        return chargeBoxRepository.findByChargeBoxId(chargePointName);
    }

    @Override
    public List<ChargeBox> getAllChargers() {
        return chargeBoxRepository.findAll();
    }

    @Override
    public Optional<ChargeBox> getChargerById(String chargeBoxId) {
        return chargeBoxRepository.findByChargeBoxId(chargeBoxId);
    }

    @Override
    public void updateHeartbeat(String chargeBoxId, long epochMilli) {
        chargeBoxRepository.findByChargeBoxId(chargeBoxId)
                .ifPresent(charger -> {
                    charger.setLastHeartbeatTimestamp(epochMilli);
                    chargeBoxRepository.save(charger);
                });
    }

    @Override
    public void updateStatus(String chargeBoxId, String status){
        chargeBoxRepository.findByChargeBoxId(chargeBoxId)
                .ifPresent(charger -> {
                    charger.setStatus(status);
                    chargeBoxRepository.save(charger);
                });
    }

    @Override
    public void preRegisterCharger(String charger) {
        if (chargeBoxRepository.findByChargeBoxId(charger).isPresent()) {
            throw new IllegalArgumentException("Charger already pre-registered!");
        }
        ChargeBox chargeBox = new ChargeBox();
        chargeBox.setChargeBoxId(charger);
        chargeBox.setRegistrationStatus("Pending");
        chargeBox.setStatus("Unavailable");
        chargeBoxRepository.save(chargeBox);
    }

    @Override
    public Optional<ChargeBox> getChargerBySerialNumber(String serialNumber) {
        return chargeBoxRepository.findByChargeBoxSerialNumber(serialNumber);
    }


}
