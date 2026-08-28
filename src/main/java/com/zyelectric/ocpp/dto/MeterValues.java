package com.zyelectric.ocpp.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class MeterValues {

    @NotNull
    private Integer connectorId;

    private Integer transactionId;

    @NotEmpty
    private List<MeterValue> meterValue;
}
