package com.zyelectric.ocpp.service;

import com.zyelectric.ocpp.model.ChargeBox;
import com.zyelectric.ocpp.model.Connector;
import com.zyelectric.ocpp.model.IdTag;
import com.zyelectric.ocpp.repository.ChargeBoxRepository;
import com.zyelectric.ocpp.repository.ConnectorRepository;
import com.zyelectric.ocpp.repository.IdTagRepository;
import com.zyelectric.ocpp.repository.StartTransactionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Proves StartTransactionService#startTransaction is actually transactional: if the insert
 * fails after the atomic activeTransactionCount increment has already run, the increment
 * itself must be rolled back too, not left applied with no matching transaction row.
 */
@SpringBootTest
@ActiveProfiles("test")
class StartTransactionServiceIntegrationTest {

    @Autowired
    private StartTransactionService startTransactionService;
    @Autowired
    private IdTagRepository idTagRepository;
    @Autowired
    private ChargeBoxRepository chargeBoxRepository;
    @Autowired
    private ConnectorRepository connectorRepository;

    @MockitoBean
    private StartTransactionRepository startTransactionRepository;

    @Test
    void startTransaction_failsAfterCountIncrement_rollsBackTheIncrement() {
        ChargeBox chargeBox = new ChargeBox();
        chargeBox.setChargeBoxId("CP-ROLLBACK");
        chargeBox.setRegistrationStatus("Accepted");
        chargeBox.setStatus("Available");
        chargeBoxRepository.save(chargeBox);

        Connector connector = new Connector();
        connector.setChargeBox(chargeBox);
        connector.setConnectorId(1);
        connectorRepository.save(connector);

        IdTag tag = new IdTag();
        tag.setIdTag("TAG-ROLLBACK");
        tag.setExpiryDate(System.currentTimeMillis() + 100_000);
        tag.setMaxActiveTransactionCount(5);
        tag.setActiveTransactionCount(0);
        tag.setBlocked(false);
        idTagRepository.save(tag);

        when(startTransactionRepository.findOpenTransactionForConnector(any())).thenReturn(Optional.empty());
        when(startTransactionRepository.save(any())).thenThrow(new RuntimeException("simulated DB failure on insert"));

        com.zyelectric.ocpp.dto.StartTransaction dto = new com.zyelectric.ocpp.dto.StartTransaction();
        dto.setIdTag("TAG-ROLLBACK");
        dto.setConnectorId(1);
        dto.setMeterStart(0.0);
        dto.setTimestamp("2026-01-01T00:00:00Z");

        assertThatThrownBy(() -> startTransactionService.startTransaction(chargeBox, dto))
                .isInstanceOf(RuntimeException.class);

        IdTag reloaded = idTagRepository.findByIdTag("TAG-ROLLBACK").orElseThrow();
        assertThat(reloaded.getActiveTransactionCount()).isEqualTo(0);
    }
}
