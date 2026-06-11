package org.example.testasks.external;

import org.example.testasks.exception.NotFoundException;
import org.example.testasks.external.dto.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Component
public class ExternalBankClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final OAuthService oAuthService;
    private final ExternalErrorHandler externalErrorHandler;

    public ExternalBankClient(
            RestTemplate restTemplate,
            @Value("${external.bank.base-url:http://localhost:8084/mock/api}") String baseUrl, OAuthService oAuthService, ExternalErrorHandler externalErrorHandler
    ) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        this.oAuthService = oAuthService;
        this.externalErrorHandler = externalErrorHandler;
    }

    private HttpHeaders authHeaders() {

        String token = oAuthService.getAccessToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        return headers;
    }


    public ExternalBalanceDto getBalance(String iban) {
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders());

        try {
            ResponseEntity<ExternalBalanceDto> response =
                    restTemplate.exchange(
                            baseUrl + "/accounts/{iban}/balance",
                            HttpMethod.GET,
                            entity,
                            ExternalBalanceDto.class,
                            iban
                    );

            return response.getBody();

        } catch (RestClientResponseException ex) {
            externalErrorHandler.handle(ex);
            throw new IllegalStateException("unreachable");
        }
    }

    public List<ExternalTransactionDto> getTransactions(String iban) {

        HttpEntity<Void> entity =
                new HttpEntity<>(authHeaders());
        try {

            ResponseEntity<List<ExternalTransactionDto>> response =
                    restTemplate.exchange(
                            baseUrl + "/accounts/{iban}/transactions",
                            HttpMethod.GET,
                            entity,
                            new ParameterizedTypeReference<>() {
                            },
                            iban
                    );
            return response.getBody();
        } catch (RestClientResponseException ex) {
            externalErrorHandler.handle(ex);
            throw new IllegalStateException("unreachable");
        }


    }

    public PaymentResponseDto initiatePayment(
            PaymentRequestDto req) {

        HttpEntity<PaymentRequestDto> entity =
                new HttpEntity<>(req, authHeaders());
        try {

            ResponseEntity<PaymentResponseDto> response =
                    restTemplate.exchange(
                            baseUrl + "/payments",
                            HttpMethod.POST,
                            entity,
                            PaymentResponseDto.class
                    );

            return response.getBody();
        } catch (RestClientResponseException ex) {
            externalErrorHandler.handle(ex);
            throw new IllegalStateException("unreachable");
        }
    }


}
