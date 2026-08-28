package com.zyelectric.ocpp.scheduler;

import com.zyelectric.ocpp.cache.WebSocketSessionCache;
import com.zyelectric.ocpp.model.ChargeBox;
import com.zyelectric.ocpp.repository.ChargeBoxRepository;
import com.zyelectric.ocpp.repository.ConnectorStatusRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatusSchedulerTest {

    @Mock
    private ChargeBoxRepository chargeBoxRepository;
    @Mock
    private ConnectorStatusRepository connectorStatusRepository;

    @InjectMocks
    private StatusScheduler statusScheduler;

    @AfterEach
    void tearDown() {
        WebSocketSessionCache.removeSession("CP-STALE");
    }

    @Test
    void markInactiveChargersAsUnavailable_flipsAvailableChargerToUnavailable() {
        // Regression test for the inverted condition bug: the original code only re-saved a
        // charger that was ALREADY "Unavailable", so a healthy-turned-stale charger never
        // actually got marked unavailable.
        ChargeBox charger = new ChargeBox();
        charger.setChargeBoxId("CP-STALE");
        charger.setStatus("Available");

        when(chargeBoxRepository.findByLastHeartbeatTimestampBefore(anyLong())).thenReturn(List.of(charger));

        statusScheduler.markInactiveChargersAsUnavailable();

        ArgumentCaptor<ChargeBox> captor = ArgumentCaptor.forClass(ChargeBox.class);
        verify(chargeBoxRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("Unavailable");
    }

    @Test
    void markInactiveChargersAsUnavailable_evictsStaleWebSocketSession() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(true);
        WebSocketSessionCache.addSession("CP-STALE", session);

        ChargeBox charger = new ChargeBox();
        charger.setChargeBoxId("CP-STALE");
        charger.setStatus("Charging");

        when(chargeBoxRepository.findByLastHeartbeatTimestampBefore(anyLong())).thenReturn(List.of(charger));

        statusScheduler.markInactiveChargersAsUnavailable();

        verify(session).close(any());
        assertThat(WebSocketSessionCache.getSessionData("CP-STALE")).isNull();
    }

    @Test
    void markInactiveChargersAsUnavailable_alreadyUnavailable_skipsRedundantSave() {
        ChargeBox charger = new ChargeBox();
        charger.setChargeBoxId("CP-STALE");
        charger.setStatus("Unavailable");

        when(chargeBoxRepository.findByLastHeartbeatTimestampBefore(anyLong())).thenReturn(List.of(charger));

        statusScheduler.markInactiveChargersAsUnavailable();

        verify(chargeBoxRepository, never()).save(any());
    }

    @Test
    void markInactiveChargersAsUnavailable_noInactiveChargers_doesNothing() {
        when(chargeBoxRepository.findByLastHeartbeatTimestampBefore(anyLong())).thenReturn(List.of());

        statusScheduler.markInactiveChargersAsUnavailable();

        verify(chargeBoxRepository, never()).save(any());
    }
}
