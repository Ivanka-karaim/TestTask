package org.example.testasks.external.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class MockAccountDto {

    private String iban;
    private String name;
    private BigDecimal balance;
    private String currency;
    private List<ExternalTransactionDto> transactions;
}
