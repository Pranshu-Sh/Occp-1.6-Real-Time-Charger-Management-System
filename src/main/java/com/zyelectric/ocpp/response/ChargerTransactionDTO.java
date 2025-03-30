package com.zyelectric.ocpp.response;

import lombok.Data;
import java.util.List;

@Data
public class ChargerTransactionDTO {
    private String chargeBoxId;
    private List<ConnectorDTO> connectors;
}
