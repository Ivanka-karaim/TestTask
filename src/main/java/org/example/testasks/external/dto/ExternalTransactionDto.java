package org.example.testasks.external.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Setter
@Getter
public class ExternalTransactionDto {
    private String id;
    private String iban;
    private BigDecimal amount;
    private String currency;
    private OffsetDateTime timestamp;
    private String description;

    public ExternalTransactionDto() {
    }

    public ExternalTransactionDto(String id, String iban, BigDecimal amount, String currency, OffsetDateTime timestamp, String description) {
        this.id = id;
        this.iban = iban;
        this.amount = amount;
        this.currency = currency;
        this.timestamp = timestamp;
        this.description = description;
    }

}
