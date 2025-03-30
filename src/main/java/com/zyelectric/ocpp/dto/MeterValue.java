package com.zyelectric.ocpp.dto;

import lombok.Data;

import java.util.List;

@Data
public class MeterValue {
    private String timestamp;
    private List<SampledValue> sampledValue;
}
