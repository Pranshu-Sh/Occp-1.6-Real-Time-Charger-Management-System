package com.zyelectric.ocpp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@Entity
@Table(name = "connector_meter_value")
@IdClass(MeterValueId.class)  // ✅ Composite PK
public class MeterValue {

    @Id
    @Column(name = "transaction_id", nullable = false)
    private Integer transactionId;   // ✅ Use Long for composite key

    @Id
    @Column(name = "value_timestamp", nullable = false)
    private Long valueTimestamp;   // ✅ Composite key part

    // ✅ Mapping to StartTransaction
    @ManyToOne(fetch = FetchType.LAZY)   // Use Lazy Loading for performance
    @JoinColumn(name = "transaction_id", referencedColumnName = "transaction_id", insertable = false, updatable = false)
    private StartTransaction startTransaction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "connector_id", referencedColumnName = "connector_pk", nullable = false)
    private Connector connector;       // Connector reference

    @Id
    @Column(name = "value", nullable = false)
    private String value;              // Meter value

    @Column(name = "reading_context")
    private String readingContext;     // Reading context (e.g., Sample.Periodic)

    @Column(name = "measurand")
    private String measurand;          // Measurand (Wh, kWh, etc.)

    @Column(name = "location")
    private String location;           // Physical location (optional)

    @Column(name = "unit")
    private String unit;               // Unit (Wh, kWh, etc.)

}
