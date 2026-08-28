package com.zyelectric.ocpp.service;

import com.zyelectric.ocpp.model.Connector;
import com.zyelectric.ocpp.model.IdTag;
import com.zyelectric.ocpp.model.StartTransaction;
import com.zyelectric.ocpp.repository.StartTransactionRepository;
import com.zyelectric.ocpp.repository.StopTransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StopTransactionServiceTest {

    @Mock
    private StopTransactionRepository stopTransactionRepository;
    @Mock
    private IdTagService idTagService;
    @Mock
    private StartTransactionRepository startTransactionRepository;

    @InjectMocks
    private StopTransactionService stopTransactionService;

    private com.zyelectric.ocpp.dto.StopTransaction dto() {
        com.zyelectric.ocpp.dto.StopTransaction dto = new com.zyelectric.ocpp.dto.StopTransaction();
        dto.setTransactionId(42);
        dto.setIdTag("TAG001");
        dto.setMeterStop(1000);
        dto.setTimestamp("2026-01-01T01:00:00Z");
        dto.setReason("Local");
        return dto;
    }

    @Test
    void stopTransaction_succeeds_clearsInTransactionFlag() {
        StartTransaction started = new StartTransaction();
        started.setTransactionId(42);
        started.setConnector(new Connector());

        IdTag tag = new IdTag();
        tag.setIdTag("TAG001");
        tag.setInTransaction(true);

        when(startTransactionRepository.findByTransactionId(42)).thenReturn(Optional.of(started));
        when(idTagService.getTagById("TAG001")).thenReturn(Optional.of(tag));

        stopTransactionService.stopTransaction(dto());

        verify(stopTransactionRepository).save(any());
        ArgumentCaptor<IdTag> captor = ArgumentCaptor.forClass(IdTag.class);
        verify(idTagService).updateTag(captor.capture());
        assertThat(captor.getValue().getInTransaction()).isFalse();
    }

    @Test
    void stopTransaction_unknownTransactionId_throwsCleanly() {
        when(startTransactionRepository.findByTransactionId(42)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> stopTransactionService.stopTransaction(dto()))
                .isInstanceOf(IllegalArgumentException.class);

        verify(stopTransactionRepository, never()).save(any());
    }

    @Test
    void stopTransaction_unknownIdTag_doesNotThrow_justSkipsTagUpdate() {
        StartTransaction started = new StartTransaction();
        started.setTransactionId(42);
        started.setConnector(new Connector());

        when(startTransactionRepository.findByTransactionId(42)).thenReturn(Optional.of(started));
        when(idTagService.getTagById("TAG001")).thenReturn(Optional.empty());

        stopTransactionService.stopTransaction(dto());

        verify(stopTransactionRepository).save(any());
        verify(idTagService, never()).updateTag(any());
    }
}
