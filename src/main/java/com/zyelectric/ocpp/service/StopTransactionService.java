package com.zyelectric.ocpp.service;

import com.zyelectric.ocpp.model.Connector;
import com.zyelectric.ocpp.model.IdTag;
import com.zyelectric.ocpp.model.StartTransaction;
import com.zyelectric.ocpp.model.StopTransaction;
import com.zyelectric.ocpp.repository.StartTransactionRepository;
import com.zyelectric.ocpp.repository.StopTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static com.zyelectric.ocpp.utils.CommonUtils.convertIso8601ToEpoch;

@Service
@RequiredArgsConstructor
public class StopTransactionService {

    private final StopTransactionRepository stopTransactionRepository;
    private final IdTagService idTagService;
    private final StartTransactionRepository startTransactionRepository;

    @Transactional
    public void stopTransaction(com.zyelectric.ocpp.dto.StopTransaction stopTransaction) {

        StartTransaction startTransaction = startTransactionRepository
                .findByTransactionId(stopTransaction.getTransactionId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No StartTransaction found for transactionId: " + stopTransaction.getTransactionId()));

        Connector connector = startTransaction.getConnector();
        StopTransaction tx = new StopTransaction();
        tx.setTransactionId(stopTransaction.getTransactionId());
        tx.setConnector(connector);
        tx.setMeterStop(stopTransaction.getMeterStop());
        tx.setEventTimestamp(convertIso8601ToEpoch(stopTransaction.getTimestamp()));
        tx.setReason(stopTransaction.getReason());
        tx.setStopTimestamp(System.currentTimeMillis());
        stopTransactionRepository.save(tx);

        Optional<IdTag> tag = idTagService.getTagById(stopTransaction.getIdTag());
        if (tag.isPresent()) {
            tag.get().setInTransaction(false);
            idTagService.updateTag(tag.get());
        }
    }

}
