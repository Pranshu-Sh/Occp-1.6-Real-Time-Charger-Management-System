package com.zyelectric.ocpp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatusNotification {

    @NotNull
    private Integer connectorId;

    @NotBlank
    private String status;

    @NotBlank
    private String errorCode;

    private String info;
    private String vendorId;
    private String vendorErrorCode;
    private Instant statusTimestamp;
}
