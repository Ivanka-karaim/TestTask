package org.example.testasks.api.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class PaymentInitiateResponse {
    private Long id;
    private String status;
    private String externalReference;

    public PaymentInitiateResponse() {}

    public PaymentInitiateResponse(Long id, String status, String externalReference) {
        this.id = id;
        this.status = status;
        this.externalReference = externalReference;
    }

}
