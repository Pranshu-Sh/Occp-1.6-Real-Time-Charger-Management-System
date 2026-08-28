CREATE TABLE charge_box (
    charge_box_pk INT NOT NULL AUTO_INCREMENT,
    charge_box_id VARCHAR(255) NOT NULL,
    registration_status VARCHAR(50) NOT NULL,
    charge_point_vendor VARCHAR(255),
    charge_point_model VARCHAR(255),
    charge_point_serial_number VARCHAR(255),
    charge_box_serial_number VARCHAR(255),
    fw_version VARCHAR(255),
    last_heartbeat_timestamp BIGINT,
    status VARCHAR(50),
    password_hash VARCHAR(100),
    PRIMARY KEY (charge_box_pk),
    UNIQUE KEY uk_charge_box_charge_box_id (charge_box_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE connector (
    connector_pk INT NOT NULL AUTO_INCREMENT,
    charge_box_id VARCHAR(255) NOT NULL,
    connector_id INT NOT NULL,
    PRIMARY KEY (connector_pk),
    UNIQUE KEY uk_connector_chargebox_connectorid (charge_box_id, connector_id),
    CONSTRAINT fk_connector_charge_box FOREIGN KEY (charge_box_id) REFERENCES charge_box (charge_box_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE connector_status (
    status_id BIGINT NOT NULL AUTO_INCREMENT,
    connector_id INT NOT NULL,
    status_timestamp DATETIME(6),
    status VARCHAR(50),
    error_code VARCHAR(50),
    info VARCHAR(255),
    PRIMARY KEY (status_id),
    KEY idx_connector_status_connector_id (connector_id),
    CONSTRAINT fk_connector_status_connector FOREIGN KEY (connector_id) REFERENCES connector (connector_pk)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE tags (
    id_tag_pk BIGINT NOT NULL AUTO_INCREMENT,
    id_tag VARCHAR(255) NOT NULL,
    parent_id_tag VARCHAR(255),
    expiry_date BIGINT NOT NULL,
    max_active_transaction_count INT,
    note VARCHAR(255),
    active_transaction_count INT,
    in_transaction BOOLEAN NOT NULL DEFAULT FALSE,
    blocked BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id_tag_pk),
    UNIQUE KEY uk_tags_id_tag (id_tag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE start_transaction (
    transaction_id INT NOT NULL AUTO_INCREMENT,
    id_tag VARCHAR(255) NOT NULL,
    connector_id INT NOT NULL,
    meter_start DOUBLE,
    start_timestamp BIGINT,
    PRIMARY KEY (transaction_id),
    KEY idx_start_transaction_id_tag (id_tag),
    KEY idx_start_transaction_connector_id (connector_id),
    CONSTRAINT fk_start_transaction_id_tag FOREIGN KEY (id_tag) REFERENCES tags (id_tag),
    CONSTRAINT fk_start_transaction_connector FOREIGN KEY (connector_id) REFERENCES connector (connector_pk)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE stop_transaction (
    transaction_id INT NOT NULL,
    event_timestamp BIGINT NOT NULL,
    connector_id INT NOT NULL,
    meter_stop INT,
    stop_timestamp BIGINT,
    reason VARCHAR(50),
    PRIMARY KEY (transaction_id, event_timestamp),
    KEY idx_stop_transaction_connector_id (connector_id),
    CONSTRAINT fk_stop_transaction_start FOREIGN KEY (transaction_id) REFERENCES start_transaction (transaction_id),
    CONSTRAINT fk_stop_transaction_connector FOREIGN KEY (connector_id) REFERENCES connector (connector_pk)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE connector_meter_value (
    transaction_id INT NOT NULL,
    value_timestamp BIGINT NOT NULL,
    value VARCHAR(255) NOT NULL,
    connector_id INT NOT NULL,
    reading_context VARCHAR(50),
    measurand VARCHAR(50),
    location VARCHAR(50),
    unit VARCHAR(20),
    PRIMARY KEY (transaction_id, value_timestamp, value),
    KEY idx_meter_value_connector_id (connector_id),
    CONSTRAINT fk_meter_value_start_transaction FOREIGN KEY (transaction_id) REFERENCES start_transaction (transaction_id),
    CONSTRAINT fk_meter_value_connector FOREIGN KEY (connector_id) REFERENCES connector (connector_pk)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
