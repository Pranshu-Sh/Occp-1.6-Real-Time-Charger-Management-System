package com.zyelectric.ocpp.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IdTagRequest {

    @NotBlank
    @Size(max = 255)
    private String idTag;

    @Size(max = 255)
    private String parentIdTag;

    @NotNull
    private Long expiryDate;

    private Integer maxActiveTransactionCount;

    @Size(max = 255)
    private String note;

    private Integer activeTransactionCount;

    private Boolean inTransaction;

    private Boolean blocked;
}
