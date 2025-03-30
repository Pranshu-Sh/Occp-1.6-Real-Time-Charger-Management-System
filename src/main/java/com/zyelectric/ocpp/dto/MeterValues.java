package com.zyelectric.ocpp.dto;

import lombok.Data;
import java.util.List;

@Data
public class MeterValues {
    private Integer connectorId;
    private Integer transactionId;
    private List<MeterValue> meterValue;
}
