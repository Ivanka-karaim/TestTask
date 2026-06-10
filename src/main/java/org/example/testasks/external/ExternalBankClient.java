package org.example.testasks.external;

import org.example.testasks.exception.NotFoundException;
import org.example.testasks.external.dto.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
// TODO додати обробку помилок

@Component
public class ExternalBankClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public ExternalBankClient(
            RestTemplate restTemplate,
            @Value("${external.bank.base-url:http://localhost:8084/mock/api}") String baseUrl
    ) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }


    public ExternalBalanceDto getBalance(String iban) {
        return restTemplate.getForObject(
                baseUrl + "/accounts/{iban}/balance",
                ExternalBalanceDto.class,
                iban
        );

    }

    public List<ExternalTransactionDto> getTransactions(String iban) {
        ResponseEntity<List<ExternalTransactionDto>> response =
                restTemplate.exchange(
                        baseUrl + "/accounts/{iban}/transactions",
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<>() {
                        },
                        iban
                );

        return response.getBody();
    }

    public PaymentResponseDto initiatePayment(PaymentRequestDto req) {
        return restTemplate.postForObject(
                baseUrl + "/payments",
                req,
                PaymentResponseDto.class
        );
    }


}
