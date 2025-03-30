package com.zyelectric.ocpp.service;

import com.zyelectric.ocpp.model.ChargeBox;
import com.zyelectric.ocpp.model.Connector;
import com.zyelectric.ocpp.model.IdTag;
import com.zyelectric.ocpp.model.StartTransaction;
import com.zyelectric.ocpp.repository.StartTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static com.zyelectric.ocpp.utils.CommonUtils.convertIso8601ToEpoch;

@Service
@RequiredArgsConstructor
public class StartTransactionService {

    private final StartTransactionRepository startTransactionRepository;
    private final IdTagService idTagService;
    private final ConnectorService connectorService;

    public StartTransaction startTransaction(ChargeBox chargeBox, com.zyelectric.ocpp.dto.StartTransaction startTransaction) {

        IdTag tag = idTagService.getTagById(startTransaction.getIdTag())
                .orElseThrow(() -> new RuntimeException("Invalid ID Tag"));

        Optional<Connector> connector = connectorService.getConnector(chargeBox, startTransaction.getConnectorId());

        if (tag.getMaxActiveTransactionCount() == 0) {
            throw new RuntimeException("ID Tag: " + tag.getIdTag() + " is blocked and cannot start transactions.");
        }

        if (tag.getMaxActiveTransactionCount() > 0 && tag.getActiveTransactionCount() >= tag.getMaxActiveTransactionCount()) {
            throw new RuntimeException("Max transactions reached for ID Tag: " + tag.getIdTag());
        }

        StartTransaction tx = new StartTransaction();
        tx.setConnector(connector.get());
        tx.setIdTag(tag);
        tx.setMeterStart(startTransaction.getMeterStart());
        tx.setStartTimestamp(convertIso8601ToEpoch(startTransaction.getTimestamp()));

        startTransactionRepository.save(tx);

        if (tag.getMaxActiveTransactionCount() >= 0) {
            tag.setActiveTransactionCount((tag.getActiveTransactionCount() + 1));
        }
        tag.setInTransaction(true);
        idTagService.updateTag(tag);

        return tx;
    }
}
