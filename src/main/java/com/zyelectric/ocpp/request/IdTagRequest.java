package com.zyelectric.ocpp.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IdTagRequest {
    private String idTag;
    private String parentIdTag;
    private Long expiryDate;
    private Integer maxActiveTransactionCount;
    private String note;
    private Integer activeTransactionCount;
    private Boolean inTransaction;
    private Boolean blocked;
}
