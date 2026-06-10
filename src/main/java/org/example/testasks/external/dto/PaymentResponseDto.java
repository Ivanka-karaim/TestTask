package org.example.testasks.external.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class PaymentResponseDto {
    private String status;
    private String externalReference;

    public PaymentResponseDto() {
    }

    public PaymentResponseDto(String status, String externalReference) {
        this.status = status;
        this.externalReference = externalReference;
    }

}
