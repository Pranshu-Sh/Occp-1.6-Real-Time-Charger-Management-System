package com.zyelectric.ocpp.repository;

import com.zyelectric.ocpp.model.IdTag;
import com.zyelectric.ocpp.model.StartTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StartTransactionRepository extends JpaRepository<StartTransaction, Long> {

    Optional<StartTransaction> findByTransactionId(Integer transactionId);

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
