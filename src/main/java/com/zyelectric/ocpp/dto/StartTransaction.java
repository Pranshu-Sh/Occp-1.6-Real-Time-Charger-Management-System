package com.zyelectric.ocpp.dto;

import lombok.Data;
import java.time.Instant;

@Data
public class StartTransaction {
    private String idTag;
    private int connectorId;
    private Double meterStart;
    private String timestamp;
}
