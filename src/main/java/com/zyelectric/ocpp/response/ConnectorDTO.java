package com.zyelectric.ocpp.response;

import lombok.Data;
import java.util.List;

@Data
public class ConnectorDTO {
    private Integer connectorId;
    private List<com.zyelectric.ocpp.dto.MeterValueDTO> meterValues;
}
