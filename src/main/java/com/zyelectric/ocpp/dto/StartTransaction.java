package com.zyelectric.ocpp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class StartTransaction {

    @NotBlank
    private String idTag;

    // Connector 0 is reserved by OCPP 1.6 for "the whole Charge Point" - a transaction can
    // only be started on a real connector, 1 or above.
    @Positive
    private int connectorId;

    @NotNull
    private Double meterStart;

    @NotBlank
    private String timestamp;
}
