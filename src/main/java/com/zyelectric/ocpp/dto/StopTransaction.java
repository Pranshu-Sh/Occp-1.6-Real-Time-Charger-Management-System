package com.zyelectric.ocpp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class StopTransaction {

    // idTag is OPTIONAL per OCPP 1.6 spec (a charger may stop a transaction without
    // re-presenting a tag) - do not require it here.
    private String idTag;

    @NotNull
    private Integer meterStop;

    @NotBlank
    private String timestamp;

    private String reason;

    @NotNull
    private Integer transactionId;

    private List<TransactionData> transactionData;

}
