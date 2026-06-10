package org.example.testasks.api.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
public class AccountBalanceResponse {
    private String iban;
    private BigDecimal balance;
    private String currency;

    public AccountBalanceResponse() {}

    public AccountBalanceResponse(String iban, BigDecimal balance, String currency) {
        this.iban = iban;
        this.balance = balance;
        this.currency = currency;
    }

}
