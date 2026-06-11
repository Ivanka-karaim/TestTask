package org.example.testasks.external.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MockAccountDto {

    private String iban;
    private String name;
    private BigDecimal balance;
    private String currency;
    private List<ExternalTransactionDto> transactions;
}
