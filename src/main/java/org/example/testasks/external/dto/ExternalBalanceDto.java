package org.example.testasks.external.dto;

import lombok.*;

import java.math.BigDecimal;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ExternalBalanceDto {
    private String iban;
    private String name;
    private BigDecimal balance;
    private String currency;
}
