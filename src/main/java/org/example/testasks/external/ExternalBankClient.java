package org.example.testasks.external;

import org.example.testasks.exception.NotFoundException;
import org.example.testasks.external.dto.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
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
    private final OAuthService oAuthService;

    public ExternalBankClient(
            RestTemplate restTemplate,
            @Value("${external.bank.base-url:http://localhost:8084/mock/api}") String baseUrl, OAuthService oAuthService
    ) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        this.oAuthService = oAuthService;
    }

    private HttpHeaders authHeaders() {

        String token = oAuthService.getAccessToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        return headers;
    }


    public ExternalBalanceDto getBalance(String iban) {

        HttpEntity<Void> entity =
                new HttpEntity<>(authHeaders());

        ResponseEntity<ExternalBalanceDto> response =
                restTemplate.exchange(
                        baseUrl + "/accounts/{iban}/balance",
                        HttpMethod.GET,
                        entity,
                        ExternalBalanceDto.class,
                        iban
                );

        return response.getBody();
    }

    public List<ExternalTransactionDto> getTransactions(String iban) {

        HttpEntity<Void> entity =
                new HttpEntity<>(authHeaders());

        ResponseEntity<List<ExternalTransactionDto>> response =
                restTemplate.exchange(
                        baseUrl + "/accounts/{iban}/transactions",
                        HttpMethod.GET,
                        entity,
                        new ParameterizedTypeReference<>() {},
                        iban
                );

        return response.getBody();
    }

    public PaymentResponseDto initiatePayment(
            PaymentRequestDto req) {

        HttpEntity<PaymentRequestDto> entity =
                new HttpEntity<>(req, authHeaders());

        ResponseEntity<PaymentResponseDto> response =
                restTemplate.exchange(
                        baseUrl + "/payments",
                        HttpMethod.POST,
                        entity,
                        PaymentResponseDto.class
                );

        return response.getBody();
    }


}
