package com.zyelectric.ocpp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatusNotification {

    private Integer connectorId;
    private String status;
    private String errorCode;
    private String info;
    private String vendorId;
    private String vendorErrorCode;
    private Instant statusTimestamp;
}
