package com.zyelectric.ocpp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.zyelectric.ocpp.dto.TransactionData;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Data
public class StopTransaction {

    private String idTag;

    private Integer meterStop;

    private String timestamp;

    private String reason;

    private Integer transactionId;

    private List<TransactionData> transactionData;

}
