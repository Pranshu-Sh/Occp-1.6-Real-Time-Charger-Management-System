package com.zyelectric.ocpp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zyelectric.ocpp.cache.SessionData;
import com.zyelectric.ocpp.dto.BootNotification;
import com.zyelectric.ocpp.dto.MeterValues;
import com.zyelectric.ocpp.dto.StatusNotification;
import com.zyelectric.ocpp.dto.StopTransaction;
import com.zyelectric.ocpp.model.ChargeBox;
import com.zyelectric.ocpp.model.Connector;
import com.zyelectric.ocpp.model.ConnectorStatus;
import com.zyelectric.ocpp.model.IdTag;
import com.zyelectric.ocpp.model.StartTransaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.zyelectric.ocpp.cache.WebSocketSessionCache.getSessionData;
import static com.zyelectric.ocpp.utils.CommonUtils.convertEpochToIso8601;

@Slf4j
@Service
@RequiredArgsConstructor
public class OcppMessageProcessor {
    private static final double DEFAULT_METER_START = 0.0;
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final ChargerService chargerService;
    private final ConnectorService connectorService;
    private final ConnectorStatusService connectorStatusService;
    private final IdTagService idTagService;
    private final StartTransactionService startTransactionService;
    private final MeterValueService meterValueService;
    private final StopTransactionService stopTransactionService;
    public void processMessage(String chargePointName, String rawMessage, WebSocketSession session) {
        try {
            List<?> messageList = objectMapper.readValue(rawMessage, List.class);

            if (messageList.size() < 3) {
                log.error("Invalid OCPP message format: {}", rawMessage);
                return;
            }

            String messageId = (String) messageList.get(1);
            String action = (String) messageList.get(2);

            Map<String, Object> payload = messageList.size() > 3 ? (Map<String, Object>) messageList.get(3) : Map.of();
            WebSocketSession cachedSession = getSessionData(chargePointName);

            if (cachedSession == null) {
                log.error("No session data found for: {}", chargePointName);
                return;
            }

            switch (action) {
                case "BootNotification" -> handleBootNotification(chargePointName, cachedSession, messageId, payload);
                case "StatusNotification" ->
                        handleStatusNotification(chargePointName, cachedSession, messageId, payload);
                case "Heartbeat" -> handleHeartbeat(chargePointName, cachedSession, messageId);
                case "Authorize" -> handleAuthorize(cachedSession, messageId, payload);
                case "StartTransaction" ->
                        handleStartTransaction(chargePointName, cachedSession, messageId, payload);
                case "StopTransaction" -> handleStopTransaction(cachedSession, messageId, payload);
                case "MeterValues" -> handleMeterValues(chargePointName, cachedSession, messageId, payload);
//                case "FirmwareStatusNotification" ->
//                        handleFirmwareStatusNotification(chargePointName, payload, session, messageId);
//                case "DiagnosticsStatusNotification" ->
//                        handleDiagnosticsStatusNotification(chargePointName, payload, session, messageId);
//                case "GetConfiguration" -> handleGetConfiguration(chargePointName, payload, session, messageId);
//                case "ChangeConfiguration" -> handleChangeConfiguration(chargePointName, payload, session, messageId);
//                case "RemoteStartTransaction" ->
//                        handleRemoteStartTransaction(chargePointName, payload, session, messageId);
//                case "RemoteStopTransaction" ->
//                        handleRemoteStopTransaction(chargePointName, payload, session, messageId);
//                case "Reset" -> handleReset(chargePointName, payload, session, messageId);
//                case "UnlockConnector" -> handleUnlockConnector(chargePointName, payload, session, messageId);
//                case "ClearCache" -> handleClearCache(chargePointName, session, messageId);
//                case "TriggerMessage" -> handleTriggerMessage(chargePointName, payload, session, messageId);
//                case "GetDiagnostics" -> handleGetDiagnostics(chargePointName, payload, session, messageId);
//                case "UpdateFirmware" -> handleUpdateFirmware(chargePointName, payload, session, messageId);
                default -> log.warn("Unknown OCPP action: {}", action);
            }

        } catch (IOException e) {
            log.error("Failed to parse OCPP message: {}", e.getMessage());
        }
    }

    private void handleBootNotification(String chargePointName, WebSocketSession session, String messageId, Map<String, Object> payload) throws IOException {
        BootNotification bootNotification = objectMapper.convertValue(payload, BootNotification.class);
        Optional<ChargeBox> chargeBox = chargerService.getChargerById(chargePointName);
        List<Object> response;
        if (chargeBox.isPresent()) {
            chargeBox = chargerService.registerCharger(chargePointName, bootNotification);
            response = Arrays.asList(
                    3,
                    messageId,
                    Map.of(
                            "status", chargeBox.get().getRegistrationStatus(),
                            "currentTime", convertEpochToIso8601(System.currentTimeMillis()),
                            "interval", "300"
                    )
            );
        } else {
            response = Arrays.asList(
                    3,
                    messageId,
                    Map.of(
                            "status", "Rejected",
                            "currentTime", convertEpochToIso8601(System.currentTimeMillis()),
                            "interval", "0"
                    )
            );
            log.warn("Unauthorized charger: {} - Rejected", chargePointName);
        }
        session.sendMessage(new TextMessage(toJsonString(response)));
        log.info("Sent BootNotification response: {}", toJsonString(response));
    }

    private void handleStatusNotification(String chargePointName, WebSocketSession session, String messageId, Map<String, Object> payload) throws IOException {
        StatusNotification statusNotification = objectMapper.convertValue(payload, StatusNotification.class);

        Optional<ChargeBox> chargeBoxOpt = chargerService.getChargerById(chargePointName);

        if (chargeBoxOpt.isPresent()) {
            ChargeBox chargeBox = chargeBoxOpt.get();

            // ✅ Register or retrieve the connector
            Optional<Connector> connectorOpt = connectorService.registerConnector(chargeBox, statusNotification);

            connectorOpt.ifPresent(connector -> {
                // ✅ Save status separately
                connectorStatusService.saveStatus(connector, statusNotification);

                log.info("StatusNotification processed for Connector ID: {}, Status: {}, Error: {}",
                        statusNotification.getConnectorId(),
                        statusNotification.getStatus(),
                        statusNotification.getErrorCode());
            });

            // ✅ Respond to CP with acknowledgment
            List<Object> response = Arrays.asList(3, messageId, Map.of());
            session.sendMessage(new TextMessage(toJsonString(response)));
        } else {
            log.warn("Unknown charger: {}", chargePointName);
            List<Object> errorResponse = Arrays.asList(4, messageId, "Unknown charger");
            session.sendMessage(new TextMessage(toJsonString(errorResponse)));
        }
    }
    private void handleHeartbeat(String chargePointName, WebSocketSession session, String messageId) throws IOException {
        long timestamp = System.currentTimeMillis();
        Map<String, Object> responsePayload = Map.of(
                "currentTime", convertEpochToIso8601(timestamp)
        );
        chargerService.updateHeartbeat(chargePointName, timestamp);
        List<Object> response = Arrays.asList(3, messageId, responsePayload);
        session.sendMessage(new TextMessage(toJsonString(response)));
        log.info("Sent Heartbeat response: {}", toJsonString(response));
    }

    // ✅ Handle Authorize
    private void handleAuthorize(WebSocketSession session, String messageId, Map<String, Object> payload) throws IOException {
        String idTag = (String) payload.get("idTag");

        log.info("Authorize request received for ID Tag: {}", idTag);

        // Validate the ID tag
        String status = idTagService.validateTag(idTag);

        Map<String, Object> authPayload = Map.of(
                "idTagInfo", Map.of(
                        "status", status
                )
        );

        List<Object> response = Arrays.asList(3, messageId, authPayload);
        session.sendMessage(new TextMessage(toJsonString(response)));
        log.info("Sent Authorize response: {}", toJsonString(response));
    }

    // ✅ Handle StartTransaction
    private void handleStartTransaction(String chargePointName, WebSocketSession session,
                                        String messageId, Map<String, Object> payload) throws IOException {
        log.info("Received StartTransaction request");
        com.zyelectric.ocpp.dto.StartTransaction startTransaction = objectMapper.convertValue(payload, com.zyelectric.ocpp.dto.StartTransaction.class);

        Optional<ChargeBox> chargeBoxOpt = chargerService.getChargerById(chargePointName);

        try {
            // Persist transaction in the database
            StartTransaction tx = startTransactionService.startTransaction(chargeBoxOpt.get(), startTransaction);

            // Prepare the response with the persisted transaction ID
            Map<String, Object> responsePayload = Map.of(
                    "idTagInfo", Map.of(
                            "status", "Accepted"
                    ),
                    "transactionId", tx.getTransactionId()
            );

            List<Object> response = Arrays.asList(3, messageId, responsePayload);
            session.sendMessage(new TextMessage(toJsonString(response)));

            log.info("Sent StartTransaction response: {}", toJsonString(response));

        } catch (Exception e) {
            log.error("Failed to process StartTransaction: {}", e.getMessage(), e);
        }
    }


    // ✅ Handle StopTransaction
    private void handleStopTransaction(WebSocketSession session, String messageId, Map<String, Object> payload) throws IOException {
        log.info("Received StopTransaction request");
        com.zyelectric.ocpp.dto.StopTransaction stopTransaction = objectMapper.convertValue(payload, StopTransaction.class);
        stopTransactionService.stopTransaction(stopTransaction);
        log.info("Stopping transaction for ID Tag: {}, Meter Stop: {}, Transaction ID: {}", stopTransaction.getIdTag(), stopTransaction.getMeterStop(), stopTransaction.getTransactionId());
        String status = idTagService.validateTag(stopTransaction.getIdTag());
        Map<String, Object> responsePayload = Map.of(
                "idTagInfo", Map.of(
                        "status", status)
        );
        List<Object> response = Arrays.asList(3, messageId, responsePayload);
        session.sendMessage(new TextMessage(toJsonString(response)));
        log.info("Sent StopTransaction response: {}", toJsonString(response));
    }

    private void handleMeterValues(String chargePointName, WebSocketSession session, String messageId, Map<String, Object> payload) throws IOException {
        log.info("Received MeterValues request: {}", payload);

        try {
            MeterValues meterValues = objectMapper.convertValue(payload, MeterValues.class);
            Optional<ChargeBox> chargeBox = chargerService.getChargerById(chargePointName);

            meterValueService.saveMeterValue(chargeBox.get(), meterValues);

            List<Object> response = Arrays.asList(3, messageId, new HashMap<>());
            session.sendMessage(new TextMessage(toJsonString(response)));
            log.info("Sent MeterValues acknowledgment: {}", toJsonString(response));

        } catch (Exception e) {
            log.error("Failed to process MeterValues", e);
            List<Object> errorResponse = Arrays.asList(4, messageId, "InternalError", e.getMessage(), new HashMap<>());
            session.sendMessage(new TextMessage(toJsonString(errorResponse)));
        }
    }

    private String toJsonString(Object obj) throws IOException {
        return objectMapper.writeValueAsString(obj);
    }
}
