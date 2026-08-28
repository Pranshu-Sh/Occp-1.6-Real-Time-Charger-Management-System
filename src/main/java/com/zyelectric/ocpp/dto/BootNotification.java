package com.zyelectric.ocpp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BootNotification {

    @NotBlank
    @Size(max = 255)
    private String chargePointVendor;

    @NotBlank
    @Size(max = 255)
    private String chargePointModel;

    @Size(max = 255)
    private String chargePointSerialNumber;

    @Size(max = 255)
    private String chargeBoxSerialNumber;

    @Size(max = 255)
    private String firmwareVersion;

    @Size(max = 255)
    private String iccid;

    @Size(max = 255)
    private String imsi;
}
