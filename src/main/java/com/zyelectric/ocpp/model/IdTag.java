package com.zyelectric.ocpp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "tags")
public class IdTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tag_pk")
    private Long id;

    @Column(name = "id_tag")
    private String idTag;

    @Column(name = "parent_id_tag")
    private String parentIdTag;

    @Column(name = "expiry_date", nullable = false)
    private Long expiryDate;

    @Column(name = "max_active_transaction_count")
    private Integer maxActiveTransactionCount;

    @Column(name = "note")
    private String note;

    @Column(name = "active_transaction_count")
    private Integer activeTransactionCount;

    @Column(name = "in_transaction")
    private Boolean inTransaction = false;

    @Column(name = "blocked")
    private Boolean blocked = false;
}
