package org.example.testasks.external.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
@AllArgsConstructor
public class ExternalBalanceDto {
    private String iban;
    private String name;
    private BigDecimal balance;
    private String currency;
}
