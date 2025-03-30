package com.zyelectric.ocpp.repository;

import com.zyelectric.ocpp.model.StopTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StopTransactionRepository extends JpaRepository<StopTransaction, Long> {

}
