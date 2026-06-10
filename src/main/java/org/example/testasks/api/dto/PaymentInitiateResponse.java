package org.example.testasks.api.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class PaymentInitiateResponse {
    private String status;
    private String externalReference;

    public PaymentInitiateResponse() {}

    public PaymentInitiateResponse( String status, String externalReference) {
        this.status = status;
        this.externalReference = externalReference;
    }

}
