package com.zyelectric.ocpp.dao;

import com.zyelectric.ocpp.model.IdTag;
import com.zyelectric.ocpp.model.StartTransaction;
import com.zyelectric.ocpp.repository.StartTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class StartTransactionDaoImpl implements StartTransactionDao {

    private final StartTransactionRepository startTransactionRepository;

    @Override
    public void save(StartTransaction startTransaction) {
        startTransactionRepository.save(startTransaction);
    }

    @Override
    public Optional<StartTransaction> findByTransactionId(Integer transactionId) {
        return startTransactionRepository.findByTransactionId(transactionId);
    }

//    @Override
//    public List<StartTransaction> findActiveTransactionsByIdTag(IdTag idTag) {
//        return startTransactionRepository.findAllByIdTagAndInTransactionTrue(idTag);
//    }

//    @Override
//    public long countActiveTransactionsByIdTag(IdTag idTag) {
//        return startTransactionRepository.countByIdTagAndInTransactionTrue(idTag);
//    }

    @Override
    public List<StartTransaction> findTransactionsByIdTagAndTimeRange(IdTag idTag, long startTime, long endTime) {
        return startTransactionRepository.findByIdTagAndStartTimeBetween(idTag, startTime, endTime);
    }

//    @Override
//    public List<StartTransaction> findAllActiveTransactions() {
//        return startTransactionRepository.findAllByInTransactionTrue();
//    }

    @Override
    public List<StartTransaction> findExpiredTransactions(long cutoffTime) {
        return startTransactionRepository.findByStartTimestampBefore(cutoffTime);
    }
}
