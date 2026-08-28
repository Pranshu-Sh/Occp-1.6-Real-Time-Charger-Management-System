package com.zyelectric.ocpp.repository;

import com.zyelectric.ocpp.model.Connector;
import com.zyelectric.ocpp.model.IdTag;
import com.zyelectric.ocpp.model.StartTransaction;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StartTransactionRepository extends JpaRepository<StartTransaction, Long> {

    Optional<StartTransaction> findByTransactionId(Integer transactionId);

    /**
     * The connector's currently open transaction, if any (no matching StopTransaction row yet),
     * row-locked so a concurrent StartTransaction for the same connector blocks until this one
     * commits - closes the retransmit-creates-a-duplicate-transaction gap.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
                SELECT st FROM StartTransaction st
                WHERE st.connector = :connector
                  AND NOT EXISTS (SELECT 1 FROM StopTransaction sp WHERE sp.transactionId = st.transactionId)
            """)
    Optional<StartTransaction> findOpenTransactionForConnector(@Param("connector") Connector connector);

    @Query("SELECT tx FROM StartTransaction tx WHERE tx.idTag = :idTag AND tx.startTimestamp BETWEEN :from AND :to")
    List<StartTransaction> findByIdTagAndStartTimeBetween(
            @Param("idTag") IdTag idTag,
            @Param("from") long from,
            @Param("to") long to
    );

    List<StartTransaction> findByStartTimestampBefore(long cutoffTime);

    @Query("""
           SELECT st FROM StartTransaction st
           WHERE st.connector.id IN :connectorIds
           AND st.startTimestamp BETWEEN :startTime AND :endTime
           """)
    List<StartTransaction> findByConnectorsAndTimeRange(
            @Param("connectorIds") List<Integer> connectorIds,
            @Param("startTime") Long startTime,
            @Param("endTime") Long endTime
    );

}
