package com.zyelectric.ocpp.dao;

import com.zyelectric.ocpp.model.ConnectorStatus;


public interface ConnectorStatusDao {

    ConnectorStatus save(com.zyelectric.ocpp.model.ConnectorStatus connectorStatus);

}
