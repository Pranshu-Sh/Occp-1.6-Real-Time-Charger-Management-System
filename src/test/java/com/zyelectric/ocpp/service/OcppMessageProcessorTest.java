package com.zyelectric.ocpp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zyelectric.ocpp.cache.WebSocketSessionCache;
import com.zyelectric.ocpp.model.ChargeBox;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Every scenario here is a direct regression test for "a charge point should never be left
 * waiting on a request that will never be answered" - each one asserts a CallResult or a
 * spec-compliant 5-element CallError was actually sent, never silence.
 */
@ExtendWith(MockitoExtension.class)
class OcppMessageProcessorTest {

    private static final String CHARGE_POINT = "CP001";

    @Mock
    private ChargerService chargerService;
    @Mock
    private ConnectorService connectorService;
    @Mock
    private ConnectorStatusService connectorStatusService;
    @Mock
    private IdTagService idTagService;
    @Mock
    private StartTransactionService startTransactionService;
    @Mock
    private MeterValueService meterValueService;
    @Mock
    private StopTransactionService stopTransactionService;

    private OcppMessageProcessor processor;
    private WebSocketSession session;
    private List<TextMessage> sentMessages;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws IOException {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        processor = new OcppMessageProcessor(chargerService, connectorService, connectorStatusService,
                idTagService, startTransactionService, meterValueService, stopTransactionService, validator);

        session = mock(WebSocketSession.class);
        sentMessages = new ArrayList<>();
        doAnswer(inv -> {
            sentMessages.add(inv.getArgument(0));
            return null;
        }).when(session).sendMessage(any(TextMessage.class));

        WebSocketSessionCache.addSession(CHARGE_POINT, session);
    }

    @AfterEach
    void tearDown() {
        WebSocketSessionCache.removeSession(CHARGE_POINT);
    }

    private JsonNode lastResponse() throws IOException {
        assertThat(sentMessages).as("a response should always be sent").isNotEmpty();
        return objectMapper.readTree(sentMessages.get(sentMessages.size() - 1).getPayload());
    }

    @Test
    void processMessage_validBootNotification_registeredCharger_returnsAccepted() throws IOException {
        ChargeBox chargeBox = new ChargeBox();
        chargeBox.setChargeBoxId(CHARGE_POINT);
        chargeBox.setRegistrationStatus("Accepted");
        when(chargerService.getChargerById(CHARGE_POINT)).thenReturn(Optional.of(chargeBox));
        when(chargerService.registerCharger(eq(CHARGE_POINT), any())).thenReturn(Optional.of(chargeBox));

        processor.processMessage(CHARGE_POINT,
                "[2,\"msg1\",\"BootNotification\",{\"chargePointVendor\":\"Acme\",\"chargePointModel\":\"X1\"}]", session);

        JsonNode resp = lastResponse();
        assertThat(resp.get(0).asInt()).isEqualTo(3);
        assertThat(resp.get(1).asText()).isEqualTo("msg1");
        assertThat(resp.get(2).get("status").asText()).isEqualTo("Accepted");
    }

    @Test
    void processMessage_bootNotification_unregisteredCharger_rejected() throws IOException {
        when(chargerService.getChargerById(CHARGE_POINT)).thenReturn(Optional.empty());

        processor.processMessage(CHARGE_POINT,
                "[2,\"msg1\",\"BootNotification\",{\"chargePointVendor\":\"Acme\",\"chargePointModel\":\"X1\"}]", session);

        JsonNode resp = lastResponse();
        assertThat(resp.get(2).get("status").asText()).isEqualTo("Rejected");
    }

    @Test
    void processMessage_malformedEnvelope_tooFewElements_sendsFormationViolationCallError() throws IOException {
        processor.processMessage(CHARGE_POINT, "[2,\"msg1\"]", session);

        JsonNode resp = lastResponse();
        assertThat(resp.get(0).asInt()).isEqualTo(4);
        assertThat(resp.get(2).asText()).isEqualTo("FormationViolation");
    }

    @Test
    void processMessage_nonCallMessageType_doesNotThrowClassCastException_sendsCallError() throws IOException {
        // A CALLRESULT frame [3, uniqueId, payload] sent to the server instead of a CALL.
        processor.processMessage(CHARGE_POINT, "[3,\"msg1\",{}]", session);

        JsonNode resp = lastResponse();
        assertThat(resp.get(0).asInt()).isEqualTo(4);
        assertThat(resp.get(2).asText()).isEqualTo("NotSupported");
    }

    @Test
    void processMessage_unknownAction_sendsNotImplementedCallError() throws IOException {
        processor.processMessage(CHARGE_POINT, "[2,\"msg1\",\"SomeUnknownAction\",{}]", session);

        JsonNode resp = lastResponse();
        assertThat(resp.get(0).asInt()).isEqualTo(4);
        assertThat(resp.get(2).asText()).isEqualTo("NotImplemented");
    }

    @Test
    void processMessage_startTransactionBlocked_sendsCallResultWithBlockedStatus_notCallError() throws IOException {
        ChargeBox chargeBox = new ChargeBox();
        chargeBox.setChargeBoxId(CHARGE_POINT);
        when(chargerService.getChargerById(CHARGE_POINT)).thenReturn(Optional.of(chargeBox));
        when(startTransactionService.startTransaction(eq(chargeBox), any()))
                .thenThrow(new IllegalStateException("ID Tag is blocked"));

        processor.processMessage(CHARGE_POINT,
                "[2,\"msg1\",\"StartTransaction\",{\"idTag\":\"TAG1\",\"connectorId\":1,\"meterStart\":0.0,\"timestamp\":\"2026-01-01T00:00:00Z\"}]",
                session);

        JsonNode resp = lastResponse();
        assertThat(resp.get(0).asInt()).isEqualTo(3); // CallResult, not CallError - this is a normal auth outcome
        assertThat(resp.get(2).get("idTagInfo").get("status").asText()).isEqualTo("Blocked");
    }

    @Test
    void processMessage_handlerThrowsRuntimeException_sendsInternalErrorCallError_notSilentDrop() throws IOException {
        when(chargerService.getChargerById(CHARGE_POINT)).thenThrow(new RuntimeException("boom"));

        processor.processMessage(CHARGE_POINT,
                "[2,\"msg1\",\"BootNotification\",{\"chargePointVendor\":\"Acme\",\"chargePointModel\":\"X1\"}]", session);

        JsonNode resp = lastResponse();
        assertThat(resp.get(0).asInt()).isEqualTo(4);
        assertThat(resp.get(2).asText()).isEqualTo("InternalError");
    }

    @Test
    void processMessage_statusNotification_unknownCharger_errorFrame_is5ElementSpecCompliant() throws IOException {
        when(chargerService.getChargerById(CHARGE_POINT)).thenReturn(Optional.empty());

        processor.processMessage(CHARGE_POINT,
                "[2,\"msg1\",\"StatusNotification\",{\"connectorId\":1,\"status\":\"Available\",\"errorCode\":\"NoError\"}]", session);

        JsonNode resp = lastResponse();
        assertThat(resp.size()).isEqualTo(5);
        assertThat(resp.get(0).asInt()).isEqualTo(4);
    }

    @Test
    void processMessage_invalidPayload_missingRequiredField_sendsCallError() throws IOException {
        // chargePointModel is required (@NotBlank) but missing here.
        processor.processMessage(CHARGE_POINT,
                "[2,\"msg1\",\"BootNotification\",{\"chargePointVendor\":\"Acme\"}]", session);

        JsonNode resp = lastResponse();
        assertThat(resp.get(0).asInt()).isEqualTo(4);
    }

    @Test
    void processMessage_answersOnTheInvokingSession_notAStaleCacheLookup() throws IOException {
        // Regression test: responses must go to the session that actually delivered the
        // request, not a fresh WebSocketSessionCache lookup by name - which can point at a
        // different (e.g. newly reconnected) session and misroute the answer.
        WebSocketSession otherCachedSession = mock(WebSocketSession.class);
        WebSocketSessionCache.addSession(CHARGE_POINT, otherCachedSession);
        try {
            processor.processMessage(CHARGE_POINT, "[2,\"msg1\",\"Heartbeat\",{}]", session);

            verify(session).sendMessage(any(TextMessage.class));
            verify(otherCachedSession, never()).sendMessage(any(TextMessage.class));
        } finally {
            WebSocketSessionCache.addSession(CHARGE_POINT, session);
        }
    }
}
