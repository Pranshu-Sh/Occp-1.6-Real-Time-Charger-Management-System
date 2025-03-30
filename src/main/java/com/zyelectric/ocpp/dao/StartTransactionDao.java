package com.zyelectric.ocpp.dao;

import com.zyelectric.ocpp.model.IdTag;
import com.zyelectric.ocpp.model.StartTransaction;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StartTransactionDao {

    void save(StartTransaction startTransaction);

    Optional<StartTransaction> findByTransactionId(Integer transactionId);

//    List<StartTransaction> findActiveTransactionsByIdTag(IdTag idTag);

//    long countActiveTransactionsByIdTag(IdTag idTag);

    List<StartTransaction> findTransactionsByIdTagAndTimeRange(IdTag idTag, long startTime, long endTime);

//    List<StartTransaction> findAllActiveTransactions();

    List<StartTransaction> findExpiredTransactions(long cutoffTime);
}
