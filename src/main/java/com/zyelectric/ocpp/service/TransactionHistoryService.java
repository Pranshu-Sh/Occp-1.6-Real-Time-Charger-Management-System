package com.zyelectric.ocpp.service;

import com.zyelectric.ocpp.dto.MeterValueDTO;
import com.zyelectric.ocpp.model.Connector;
import com.zyelectric.ocpp.model.MeterValue;
import com.zyelectric.ocpp.repository.ConnectorRepository;
import com.zyelectric.ocpp.repository.MeterValueRepository;
import com.zyelectric.ocpp.response.ChargerTransactionDTO;
import com.zyelectric.ocpp.response.ConnectorDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionHistoryService {

    private final ConnectorRepository connectorRepository;
    private final MeterValueRepository meterValueRepository;

    public ChargerTransactionDTO getTransactionHistory(String chargeBoxId, Long startTime, Long endTime) {
        // 1. Retrieve all connectors for the given charge box (charger)
        List<Connector> connectors = connectorRepository.findByChargerId(chargeBoxId);

        // 2. For each connector, fetch meter values in the time range and map to DTO
        List<ConnectorDTO> connectorDTOs = connectors.stream().map(connector -> {
            List<MeterValue> meterValues = meterValueRepository.findByConnectorAndTimeRange(
                    connector.getId(), startTime, endTime);

            List<MeterValueDTO> meterValueDTOs = meterValues.stream().map(mv -> {
                MeterValueDTO dto = new MeterValueDTO();
                dto.setValueTimestamp(mv.getValueTimestamp());
                dto.setValue(mv.getValue());
                dto.setMeasurand(mv.getMeasurand());
                dto.setLocation(mv.getLocation());
                dto.setUnit(mv.getUnit());
                dto.setContext(mv.getReadingContext());
                return dto;
            }).collect(Collectors.toList());

            ConnectorDTO cDto = new ConnectorDTO();
            cDto.setConnectorId(connector.getConnectorId());
            cDto.setMeterValues(meterValueDTOs);
            return cDto;
        }).collect(Collectors.toList());

        // 3. Build the ChargerTransactionDTO
        ChargerTransactionDTO historyDTO = new ChargerTransactionDTO();
        historyDTO.setChargeBoxId(chargeBoxId);
        historyDTO.setConnectors(connectorDTOs);
        return historyDTO;
    }
}
