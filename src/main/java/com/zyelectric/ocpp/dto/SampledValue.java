package com.zyelectric.ocpp.dto;

import lombok.Data;

@Data
public class SampledValue {
    private String value;
    private String context;
    private String measurand;
    private String location;
    private String unit;
    private String format;
    private String phase;
}
