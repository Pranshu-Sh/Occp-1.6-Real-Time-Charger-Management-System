package com.zyelectric.ocpp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zyelectric.ocpp.dto.BootNotification;
import com.zyelectric.ocpp.dto.MeterValues;
import com.zyelectric.ocpp.dto.StatusNotification;
import com.zyelectric.ocpp.dto.StopTransaction;
import com.zyelectric.ocpp.model.ChargeBox;
import com.zyelectric.ocpp.model.Connector;
import com.zyelectric.ocpp.model.StartTransaction;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
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
import java.util.Set;
import java.util.stream.Collectors;

import static com.zyelectric.ocpp.utils.CommonUtils.convertEpochToIso8601;

/**
 * Parses and dispatches OCPP-J CALL frames: {@code [2, uniqueId, action, payload]}.
 * Every code path that fails to produce a normal CallResult responds with a
 * spec-compliant 5-element CallError ({@code [4, uniqueId, errorCode, errorDescription, {}]})
 * instead of silently dropping the message - a charge point should never be left waiting
 * on a request that will never be answered.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OcppMessageProcessor {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final ChargerService chargerService;
    private final ConnectorService connectorService;
    private final ConnectorStatusService connectorStatusService;
    private final IdTagService idTagService;
    private final StartTransactionService startTransactionService;
    private final MeterValueService meterValueService;
    private final StopTransactionService stopTransactionService;
    private final Validator validator;

    public void processMessage(String chargePointName, String rawMessage, WebSocketSession session) {
        String messageId = "-1";
        try {
            List<?> messageList = objectMapper.readValue(rawMessage, List.class);

            if (messageList.size() < 3) {
                sendCallError(session, messageId, "FormationViolation", "OCPP message must have at least 3 elements");
                return;
            }

            if (messageList.get(1) instanceof String id) {
                messageId = id;
            } else {
                sendCallError(session, messageId, "FormationViolation", "UniqueId must be a string");
                return;
            }

            if (!(messageList.get(0) instanceof Integer messageTypeId) || messageTypeId != 2) {
                sendCallError(session, messageId, "NotSupported", "Only CALL (MessageTypeId 2) is supported");
                return;
            }

            if (!(messageList.get(2) instanceof String action)) {
                sendCallError(session, messageId, "FormationViolation", "Action must be a string");
                return;
            }

            Object payloadRaw = messageList.size() > 3 ? messageList.get(3) : Map.of();
            if (!(payloadRaw instanceof Map)) {
                sendCallError(session, messageId, "FormationViolation", "Payload must be an object");
                return;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = (Map<String, Object>) payloadRaw;

            // Always answer on the session that actually delivered this message - not a
            // fresh WebSocketSessionCache lookup by name, which can race a reconnect and
            // route the response to the wrong physical connection.
            switch (action) {
                case "BootNotification" -> handleBootNotification(chargePointName, session, messageId, payload);
                case "StatusNotification" ->
                        handleStatusNotification(chargePointName, session, messageId, payload);
                case "Heartbeat" -> handleHeartbeat(chargePointName, session, messageId);
                case "Authorize" -> handleAuthorize(session, messageId, payload);
                case "StartTransaction" ->
                        handleStartTransaction(chargePointName, session, messageId, payload);
                case "StopTransaction" -> handleStopTransaction(session, messageId, payload);
                case "MeterValues" -> handleMeterValues(chargePointName, session, messageId, payload);
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
                default -> {
                    log.warn("Unknown OCPP action: {}", action);
                    sendCallError(session, messageId, "NotImplemented", "Action not implemented: " + action);
                }
            }

        } catch (IOException e) {
            log.error("Failed to parse OCPP message: {}", e.getMessage());
            sendCallError(session, messageId, "FormationViolation", "Malformed JSON: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unhandled error processing OCPP message: {}", e.getMessage(), e);
            sendCallError(session, messageId, "InternalError", e.getMessage() == null ? "Internal error" : e.getMessage());
        }
    }

    private void handleBootNotification(String chargePointName, WebSocketSession session, String messageId, Map<String, Object> payload) throws IOException {
        BootNotification bootNotification = validate(objectMapper.convertValue(payload, BootNotification.class));
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
        StatusNotification statusNotification = validate(objectMapper.convertValue(payload, StatusNotification.class));

        Optional<ChargeBox> chargeBoxOpt = chargerService.getChargerById(chargePointName);

        if (chargeBoxOpt.isPresent()) {
            ChargeBox chargeBox = chargeBoxOpt.get();

            Optional<Connector> connectorOpt = connectorService.registerConnector(chargeBox, statusNotification);

            connectorOpt.ifPresent(connector -> {
                connectorStatusService.saveStatus(connector, statusNotification);

                log.info("StatusNotification processed for Connector ID: {}, Status: {}, Error: {}",
                        statusNotification.getConnectorId(),
                        statusNotification.getStatus(),
                        statusNotification.getErrorCode());
            });

            List<Object> response = Arrays.asList(3, messageId, Map.of());
            session.sendMessage(new TextMessage(toJsonString(response)));
        } else {
            log.warn("Unknown charger: {}", chargePointName);
            sendCallError(session, messageId, "GenericError", "Unknown charger: " + chargePointName);
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

    private void handleAuthorize(WebSocketSession session, String messageId, Map<String, Object> payload) throws IOException {
        String idTag = (String) payload.get("idTag");

        log.info("Authorize request received for ID Tag: {}", idTag);

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

    private void handleStartTransaction(String chargePointName, WebSocketSession session,
                                        String messageId, Map<String, Object> payload) throws IOException {
        log.info("Received StartTransaction request");
        com.zyelectric.ocpp.dto.StartTransaction startTransaction = validate(objectMapper.convertValue(payload, com.zyelectric.ocpp.dto.StartTransaction.class));

        ChargeBox chargeBox = chargerService.getChargerById(chargePointName)
                .orElseThrow(() -> new IllegalArgumentException("Unknown charger: " + chargePointName));

        Map<String, Object> responsePayload;
        try {
            StartTransaction tx = startTransactionService.startTransaction(chargeBox, startTransaction);
            responsePayload = Map.of(
                    "idTagInfo", Map.of("status", "Accepted"),
                    "transactionId", tx.getTransactionId()
            );
        } catch (IllegalStateException e) {
            // A blocked tag or a tag at its concurrent-transaction limit is a normal OCPP
            // authorization outcome, not a protocol error - answer with a CallResult carrying
            // idTagInfo.status = Blocked, not a CallError.
            log.info("StartTransaction blocked for {}: {}", chargePointName, e.getMessage());
            responsePayload = Map.of(
                    "idTagInfo", Map.of("status", "Blocked"),
                    "transactionId", 0
            );
        }

        List<Object> response = Arrays.asList(3, messageId, responsePayload);
        session.sendMessage(new TextMessage(toJsonString(response)));

        log.info("Sent StartTransaction response: {}", toJsonString(response));
    }

    private void handleStopTransaction(WebSocketSession session, String messageId, Map<String, Object> payload) throws IOException {
        log.info("Received StopTransaction request");
        StopTransaction stopTransaction = validate(objectMapper.convertValue(payload, StopTransaction.class));
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

        MeterValues meterValues = validate(objectMapper.convertValue(payload, MeterValues.class));
        ChargeBox chargeBox = chargerService.getChargerById(chargePointName)
                .orElseThrow(() -> new IllegalArgumentException("Unknown charger: " + chargePointName));

        meterValueService.saveMeterValue(chargeBox, meterValues);

        List<Object> response = Arrays.asList(3, messageId, new HashMap<>());
        session.sendMessage(new TextMessage(toJsonString(response)));
        log.info("Sent MeterValues acknowledgment: {}", toJsonString(response));
    }

    private <T> T validate(T dto) {
        Set<ConstraintViolation<T>> violations = validator.validate(dto);
        if (!violations.isEmpty()) {
            String message = violations.stream()
                    .map(v -> v.getPropertyPath() + " " + v.getMessage())
                    .collect(Collectors.joining("; "));
            throw new IllegalArgumentException(message);
        }
        return dto;
    }

    private void sendCallError(WebSocketSession session, String messageId, String errorCode, String errorDescription) {
        try {
            List<Object> errorResponse = Arrays.asList(4, messageId, errorCode, errorDescription, Map.of());
            session.sendMessage(new TextMessage(toJsonString(errorResponse)));
            log.warn("Sent CallError [{}] for {}: {}", errorCode, messageId, errorDescription);
        } catch (IOException e) {
            log.error("Failed to send CallError to session: {}", e.getMessage());
        }
    }

    private String toJsonString(Object obj) throws IOException {
        return objectMapper.writeValueAsString(obj);
    }
}
