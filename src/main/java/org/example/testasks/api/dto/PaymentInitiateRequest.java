package org.example.testasks.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
public class PaymentInitiateRequest {
    @NotBlank
    @Pattern(
            regexp = "^UA[0-9]{27}$",
            message = "Invalid IBAN format"
    )
    private String fromIban;

    @NotBlank
    @Pattern(
            regexp = "^UA[0-9]{27}$",
            message = "Invalid IBAN format"
    )

    private String toIban;

    @NotNull
    @Positive
    private BigDecimal amount;

    @NotBlank
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a 3-letter ISO 4217 code")
    private String currency;

    public PaymentInitiateRequest() {}

}
