package org.example.testasks.external.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
@AllArgsConstructor
public class PaymentRequestDto {
    private String fromIban;
    private String toIban;
    private BigDecimal amount;
    private String currency;
}
