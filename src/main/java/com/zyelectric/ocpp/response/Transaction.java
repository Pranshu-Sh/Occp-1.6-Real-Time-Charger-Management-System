package com.zyelectric.ocpp.dto;

import lombok.Data;
import java.util.List;

@Data
public class Transaction {
    private Integer transactionId;
    private String connectorId;
    private Long startTimestamp;
    private Double meterStart;
    private Long stopTimestamp;
    private Integer meterStop;
    private String reason;
    private List<MeterValue> meterValues;
}
