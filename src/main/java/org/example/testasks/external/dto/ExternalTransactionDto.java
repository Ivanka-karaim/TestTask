package org.example.testasks.external.dto;



import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


import java.math.BigDecimal;

import java.time.OffsetDateTime;


@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ExternalTransactionDto {
    private String id;
    private String iban;
    private BigDecimal amount;
    private String currency;
    private OffsetDateTime timestamp;
    private String description;
}
