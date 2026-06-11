package org.example.testasks.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
public class PaymentInitiateResponse {
    private Long id;
    private String status;
    private String externalReference;

}
