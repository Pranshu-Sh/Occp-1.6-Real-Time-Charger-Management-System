package com.zyelectric.ocpp.service;

import com.zyelectric.ocpp.model.ChargeBox;
import com.zyelectric.ocpp.model.Connector;
import com.zyelectric.ocpp.model.IdTag;
import com.zyelectric.ocpp.model.StartTransaction;
import com.zyelectric.ocpp.repository.IdTagRepository;
import com.zyelectric.ocpp.repository.StartTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StartTransactionServiceTest {

    @Mock
    private StartTransactionRepository startTransactionRepository;
    @Mock
    private IdTagRepository idTagRepository;
    @Mock
    private IdTagService idTagService;
    @Mock
    private ConnectorService connectorService;

    @InjectMocks
    private StartTransactionService startTransactionService;

    private ChargeBox chargeBox;
    private Connector connector;
    private IdTag idTag;

    @BeforeEach
    void setUp() {
        chargeBox = new ChargeBox();
        chargeBox.setChargeBoxId("CP001");

        connector = new Connector();
        connector.setId(1);
        connector.setConnectorId(1);
        connector.setChargeBox(chargeBox);

        idTag = new IdTag();
        idTag.setIdTag("TAG001");
        idTag.setActiveTransactionCount(0);
        idTag.setMaxActiveTransactionCount(1);
        idTag.setBlocked(false);
    }

    private com.zyelectric.ocpp.dto.StartTransaction dto() {
        com.zyelectric.ocpp.dto.StartTransaction dto = new com.zyelectric.ocpp.dto.StartTransaction();
        dto.setIdTag("TAG001");
        dto.setConnectorId(1);
        dto.setMeterStart(0.0);
        dto.setTimestamp("2026-01-01T00:00:00Z");
        return dto;
    }

    @Test
    void startTransaction_succeedsWhenUnderLimit() {
        when(idTagService.getTagById("TAG001")).thenReturn(Optional.of(idTag));
        when(connectorService.getConnector(chargeBox, 1)).thenReturn(Optional.of(connector));
        when(startTransactionRepository.findOpenTransactionForConnector(connector)).thenReturn(Optional.empty());
        when(idTagRepository.incrementActiveTransactionCountIfAllowed("TAG001")).thenReturn(1);
        StartTransaction saved = new StartTransaction();
        saved.setTransactionId(42);
        when(startTransactionRepository.save(any())).thenReturn(saved);

        StartTransaction result = startTransactionService.startTransaction(chargeBox, dto());

        assertThat(result.getTransactionId()).isEqualTo(42);
        verify(startTransactionRepository).save(any());
    }

    @Test
    void startTransaction_rejectsWhenAtMaxActiveTransactionCount() {
        when(idTagService.getTagById("TAG001")).thenReturn(Optional.of(idTag));
        when(connectorService.getConnector(chargeBox, 1)).thenReturn(Optional.of(connector));
        when(startTransactionRepository.findOpenTransactionForConnector(connector)).thenReturn(Optional.empty());
        when(idTagRepository.incrementActiveTransactionCountIfAllowed("TAG001")).thenReturn(0);

        assertThatThrownBy(() -> startTransactionService.startTransaction(chargeBox, dto()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Max transactions reached");

        verify(startTransactionRepository, never()).save(any());
    }

    @Test
    void startTransaction_duplicateRequestForSameConnectorReturnsExistingTransaction() {
        StartTransaction existing = new StartTransaction();
        existing.setTransactionId(7);
        existing.setIdTag(idTag); // same tag as the incoming request - a genuine retransmit

        when(idTagService.getTagById("TAG001")).thenReturn(Optional.of(idTag));
        when(connectorService.getConnector(chargeBox, 1)).thenReturn(Optional.of(connector));
        when(startTransactionRepository.findOpenTransactionForConnector(connector)).thenReturn(Optional.of(existing));

        StartTransaction result = startTransactionService.startTransaction(chargeBox, dto());

        assertThat(result.getTransactionId()).isEqualTo(7);
        verify(startTransactionRepository, never()).save(any());
        verify(idTagRepository, never()).incrementActiveTransactionCountIfAllowed(any());
    }

    @Test
    void startTransaction_openTransactionOnConnectorUnderDifferentTag_rejected() {
        StartTransaction existing = new StartTransaction();
        existing.setTransactionId(7);
        IdTag otherTag = new IdTag();
        otherTag.setIdTag("OTHER-TAG");
        existing.setIdTag(otherTag);

        when(idTagService.getTagById("TAG001")).thenReturn(Optional.of(idTag));
        when(connectorService.getConnector(chargeBox, 1)).thenReturn(Optional.of(connector));
        when(startTransactionRepository.findOpenTransactionForConnector(connector)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> startTransactionService.startTransaction(chargeBox, dto()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("different ID Tag");

        verify(startTransactionRepository, never()).save(any());
    }

    @Test
    void startTransaction_unknownIdTag_throwsAndDoesNotPersist() {
        when(idTagService.getTagById("TAG001")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> startTransactionService.startTransaction(chargeBox, dto()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid ID Tag");

        verify(startTransactionRepository, never()).save(any());
    }

    @Test
    void startTransaction_unknownConnector_throwsAndDoesNotPersist() {
        when(idTagService.getTagById("TAG001")).thenReturn(Optional.of(idTag));
        when(connectorService.getConnector(chargeBox, 1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> startTransactionService.startTransaction(chargeBox, dto()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown connector");

        verify(startTransactionRepository, never()).save(any());
    }

    @Test
    void startTransaction_blockedTag_rejected() {
        idTag.setBlocked(true);
        when(idTagService.getTagById("TAG001")).thenReturn(Optional.of(idTag));

        assertThatThrownBy(() -> startTransactionService.startTransaction(chargeBox, dto()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("blocked");

        verify(startTransactionRepository, never()).save(any());
    }

    @Test
    void startTransaction_concurrentCallsNeverExceedLimit() throws InterruptedException {
        // Stand-in for the real atomic "UPDATE ... WHERE count < max" row-level guard,
        // since this unit test has no real database - proves the service correctly
        // surfaces rejections rather than double-counting when many threads race it.
        int max = 1;
        AtomicInteger activeCount = new AtomicInteger(0);
        when(idTagService.getTagById("TAG001")).thenReturn(Optional.of(idTag));
        when(connectorService.getConnector(eq(chargeBox), any())).thenReturn(Optional.of(connector));
        when(startTransactionRepository.findOpenTransactionForConnector(any())).thenReturn(Optional.empty());
        when(idTagRepository.incrementActiveTransactionCountIfAllowed("TAG001")).thenAnswer(inv -> {
            synchronized (activeCount) {
                if (activeCount.get() < max) {
                    activeCount.incrementAndGet();
                    return 1;
                }
                return 0;
            }
        });
        when(startTransactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int threads = 10;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        AtomicInteger succeeded = new AtomicInteger(0);
        AtomicInteger rejected = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                ready.countDown();
                try {
                    go.await();
                    startTransactionService.startTransaction(chargeBox, dto());
                    succeeded.incrementAndGet();
                } catch (Exception e) {
                    rejected.incrementAndGet();
                }
            });
        }
        ready.await();
        go.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(5, TimeUnit.SECONDS)).isTrue();

        assertThat(succeeded.get()).isEqualTo(1);
        assertThat(rejected.get()).isEqualTo(threads - 1);
    }
}
