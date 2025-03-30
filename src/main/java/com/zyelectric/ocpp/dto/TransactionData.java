package com.zyelectric.ocpp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class TransactionData {

    @JsonProperty("timestamp")
    private String timestamp;

    @JsonProperty("sampledValue")
    private List<SampledValue> sampledValue;

}
