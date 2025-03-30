package com.zyelectric.ocpp.dto;

import lombok.Data;

@Data
public class MeterValueDTO {
    private Long valueTimestamp;
    private String value;
    private String measurand;
    private String location;
    private String unit;
    private String context;  // This corresponds to the reading context (e.g., "Transaction.Begin" or "Transaction.End")
}
