package org.example.testasks.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Setter
@Getter
@AllArgsConstructor
public class TransactionResponse {
    private String externalId;
    private BigDecimal amount;
    private String currency;
    private OffsetDateTime timestamp;
    private String description;

}
