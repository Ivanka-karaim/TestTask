package org.example.testasks.api.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Setter
@Getter
public class TransactionResponse {
    private String externalId;
    private BigDecimal amount;
    private String currency;
    private OffsetDateTime timestamp;
    private String description;

    public TransactionResponse() {}

    public TransactionResponse(String externalId, BigDecimal amount, String currency, OffsetDateTime timestamp, String description) {
        this.externalId = externalId;
        this.amount = amount;
        this.currency = currency;
        this.timestamp = timestamp;
        this.description = description;
    }

}
