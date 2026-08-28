package com.zyelectric.ocpp.service;

import com.zyelectric.ocpp.model.ChargeBox;
import com.zyelectric.ocpp.model.Connector;
import com.zyelectric.ocpp.model.IdTag;
import com.zyelectric.ocpp.model.StartTransaction;
import com.zyelectric.ocpp.repository.IdTagRepository;
import com.zyelectric.ocpp.repository.StartTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static com.zyelectric.ocpp.utils.CommonUtils.convertIso8601ToEpoch;

@Slf4j
@Service
@RequiredArgsConstructor
public class StartTransactionService {

    private final StartTransactionRepository startTransactionRepository;
    private final IdTagRepository idTagRepository;
    private final IdTagService idTagService;
    private final ConnectorService connectorService;

    @Transactional
    public StartTransaction startTransaction(ChargeBox chargeBox, com.zyelectric.ocpp.dto.StartTransaction startTransaction) {

        IdTag tag = idTagService.getTagById(startTransaction.getIdTag())
                .orElseThrow(() -> new IllegalArgumentException("Invalid ID Tag: " + startTransaction.getIdTag()));

        if (Boolean.TRUE.equals(tag.getBlocked())) {
            throw new IllegalStateException("ID Tag: " + tag.getIdTag() + " is blocked and cannot start transactions.");
        }

        Connector connector = connectorService.getConnector(chargeBox, startTransaction.getConnectorId())
                .orElseThrow(() -> new IllegalArgumentException("Unknown connector: " + startTransaction.getConnectorId()));

        // Pessimistic-locked lookup: guards against a charger retransmitting StartTransaction
        // (a normal OCPP-J failure mode after a lost CALLRESULT) creating a second row, and
        // against two concurrent requests for the same connector racing past this check.
        Optional<StartTransaction> existingOpen = startTransactionRepository.findOpenTransactionForConnector(connector);
        if (existingOpen.isPresent()) {
            StartTransaction open = existingOpen.get();
            if (!open.getIdTag().getIdTag().equals(tag.getIdTag())) {
                throw new IllegalStateException("Connector " + connector.getId()
                        + " already has an open transaction under a different ID Tag");
            }
            log.info("StartTransaction retransmit detected for connector {} - returning existing transaction {}",
                    connector.getId(), open.getTransactionId());
            return open;
        }

        int updated = idTagRepository.incrementActiveTransactionCountIfAllowed(tag.getIdTag());
        if (updated == 0) {
            throw new IllegalStateException("Max transactions reached for ID Tag: " + tag.getIdTag());
        }

        StartTransaction tx = new StartTransaction();
        tx.setConnector(connector);
        tx.setIdTag(tag);
        tx.setMeterStart(startTransaction.getMeterStart());
        tx.setStartTimestamp(convertIso8601ToEpoch(startTransaction.getTimestamp()));

        return startTransactionRepository.save(tx);
    }
}
