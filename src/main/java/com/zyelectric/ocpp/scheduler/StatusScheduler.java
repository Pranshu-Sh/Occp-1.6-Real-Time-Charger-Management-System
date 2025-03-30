package com.zyelectric.ocpp.scheduler;

import com.zyelectric.ocpp.model.ChargeBox;
import com.zyelectric.ocpp.repository.ChargeBoxRepository;
import com.zyelectric.ocpp.repository.ConnectorStatusRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatusScheduler {

    private final ChargeBoxRepository chargeBoxRepository;
    private final ConnectorStatusRepository connectorStatusRepository;

    @Scheduled(fixedRate = 360000)  // Every 6 minutes
    @Transactional
    public void markInactiveChargersAsUnavailable() {
        long fiveMinutesAgo = Instant.now().minus(Duration.ofMinutes(5)).toEpochMilli();

        // Fetch chargers with last heartbeat > 5 minutes ago
        List<ChargeBox> inactiveChargers = chargeBoxRepository.findByLastHeartbeatTimestampBefore(fiveMinutesAgo);

        if (inactiveChargers.isEmpty()) {
            log.info("No inactive chargers detected.");
            return;
        }

        log.info("Found {} inactive chargers. Marking as Unavailable...", inactiveChargers.size());

        for (ChargeBox charger : inactiveChargers) {
            if (charger.getStatus().equals("Unavailable")) {
                charger.setStatus("Unavailable");
                chargeBoxRepository.save(charger);

                log.info("Marked charger '{}' as Unavailable due to inactivity.", charger.getChargeBoxId());
            }

        }
    }

//    @Scheduled(fixedRate = 300000) // Every 5 minutes
//    public void updateChargerStatus() {
//        // Fetch all connectors associated with chargers
//        List<ConnectorStatus> allConnectorStatuses = connectorStatusRepository.findAll();
//
//        // Group the connectors by charger (assuming each connector has a charger associated with it)
//        allConnectorStatuses.stream()
//                .map(ConnectorStatus::getConnector)
//                .map(Connector::getChargeBox) // Get associated ChargeBox
//                .distinct()
//                .forEach(chargeBox -> {
//                    // Get all connectors for the current chargeBox (charger)
//                    List<ConnectorStatus> connectorsForCharger = connectorStatusRepository.getConnectorsForCharger(chargeBox);
//
//                    // Determine the overall status for this chargeBox based on its connectors' statuses
//                    String overallChargerStatus = determineOverallChargerStatus(connectorsForCharger);
//
//                    // Update the chargeBox status
//                    chargeBox.setStatus(overallChargerStatus);
//                    chargeBoxRepository.save(chargeBox);
//
//                    log.info("Updated charger status for Charger ID: {} to: {}",
//                            chargeBox.getChargeBoxId(), overallChargerStatus);
//                });
//    }
//
//    public static String determineOverallChargerStatus(List<ConnectorStatus> connectorStatuses) {
//        boolean hasFaulted = connectorStatuses.stream()
//                .anyMatch(c -> "Faulted".equalsIgnoreCase(c.getStatus()));
//
//        boolean hasUnavailable = connectorStatuses.stream()
//                .anyMatch(c -> "Unavailable".equalsIgnoreCase(c.getStatus()));
//
//        boolean hasCharging = connectorStatuses.stream()
//                .anyMatch(c -> "Charging".equalsIgnoreCase(c.getStatus()));
//
//        boolean allAvailable = connectorStatuses.stream()
//                .allMatch(c -> "Available".equalsIgnoreCase(c.getStatus()));
//
//        if (hasFaulted) {
//            return "Faulted";
//        } else if (hasUnavailable) {
//            return "Unavailable";
//        } else if (hasCharging) {
//            return "Charging";
//        } else if (allAvailable) {
//            return "Available";
//        } else {
//            return "Unavailable";
//        }
//    }
}
