package org.example.testasks.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
@AllArgsConstructor
public class AccountBalanceResponse {
    private String iban;
    private BigDecimal balance;
    private String currency;
}
