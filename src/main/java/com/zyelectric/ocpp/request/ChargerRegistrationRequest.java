package com.zyelectric.ocpp.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChargerRegistrationRequest {

    @NotBlank
    private String charger;

    @NotBlank
    @Size(min = 8)
    private String password;
}
