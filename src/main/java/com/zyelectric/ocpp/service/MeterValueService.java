package com.zyelectric.ocpp.service;

import com.zyelectric.ocpp.dto.MeterValues;
import com.zyelectric.ocpp.dto.SampledValue;
import com.zyelectric.ocpp.model.*;
import com.zyelectric.ocpp.repository.MeterValueRepository;
import com.zyelectric.ocpp.repository.ConnectorRepository;
import com.zyelectric.ocpp.repository.StartTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static com.zyelectric.ocpp.utils.CommonUtils.convertIso8601ToEpoch;

@Service
@RequiredArgsConstructor
public class MeterValueService {

    private final MeterValueRepository meterValueRepository;
    private final ConnectorRepository connectorRepository;
    private final StartTransactionRepository startTransactionRepository;

    @Transactional
    public void saveMeterValue(ChargeBox chargeBox, MeterValues meterValues) {

//        StartTransaction startTransaction = startTransactionRepository.f(transactionId)
//                .orElseThrow(() -> new RuntimeException("StartTransaction not found with ID: " + transactionId));

        Connector connector = connectorRepository.findById(Long.valueOf(meterValues.getConnectorId()))
                .orElseThrow(() -> new RuntimeException("Connector not found with ID: " + meterValues.getConnectorId()));
        for (com.zyelectric.ocpp.dto.MeterValue m : meterValues.getMeterValue()) {
            long timestamp = convertIso8601ToEpoch(m.getTimestamp());

            for (SampledValue s : m.getSampledValue()) {
                MeterValue meterValue = new MeterValue();
                meterValue.setTransactionId(meterValues.getTransactionId());
                meterValue.setValueTimestamp(timestamp);
                meterValue.setConnector(connector);
                meterValue.setValue(s.getValue());
                meterValue.setReadingContext(s.getContext());
                meterValue.setMeasurand(s.getMeasurand());
                meterValue.setUnit(s.getUnit());
                meterValue.setLocation(s.getLocation());

                meterValueRepository.save(meterValue);

                meterValueRepository.flush();  // Force Hibernate to flush, ensuring new insert
            }
        }

        // ✅ Create the MeterValue with composite PK

        }
}
