package org.example.testasks.external;

import org.example.testasks.api.dto.ErrorResponseDto;
import org.example.testasks.exception.NotFoundException;
import org.example.testasks.external.dto.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.io.IOException;
import java.net.ConnectException;
import java.time.Duration;
import java.util.List;

@Component
public class ExternalBankClient {

    private final WebClient webClient;
    private final int maxAttempts;
    private final long backoffMs;

    public ExternalBankClient(@org.springframework.beans.factory.annotation.Qualifier("externalBankWebClient") WebClient webClient,
                              @Value("${external.bank.retry.max-attempts:3}") int maxAttempts,
                              @Value("${external.bank.retry.backoff-ms:500}") long backoffMs) {
        this.webClient = webClient;
        this.maxAttempts = Math.max(1, maxAttempts);
        this.backoffMs = Math.max(100, backoffMs);
    }

    public ExternalBalanceDto getBalance(String iban) {
        return webClient.get()
                .uri("/accounts/{iban}/balance", iban)
                .retrieve()
                .onStatus(
                        status -> status.value() == 404,
                        response -> response.bodyToMono(ErrorResponseDto.class)
                                .flatMap(error ->
                                        Mono.error(new NotFoundException(error.message())))
                )
                .bodyToMono(ExternalBalanceDto.class)

                .retryWhen(retrySpec())
                .block();
    }

    public List<ExternalTransactionDto> getTransactions(String iban) {
        return webClient.get()
                .uri("/accounts/{iban}/transactions", iban)
                .retrieve()
                .bodyToFlux(ExternalTransactionDto.class)
                .collectList()
                .retryWhen(retrySpec())
                .block();
    }

    public PaymentResponseDto initiatePayment(PaymentRequestDto req) {
        return webClient.post()
                .uri("/payments")
                .bodyValue(req)
                .retrieve()
                .bodyToMono(PaymentResponseDto.class)
                .retryWhen(retrySpec())
                .block();
    }

    private Retry retrySpec() {
        // reactor Retry expects number of retry attempts (not total attempts)
        long retries = Math.max(0, maxAttempts - 1);
        return Retry.backoff(retries, Duration.ofMillis(backoffMs))
                .filter(throwable -> {
                    // Retry on connection failures and on 5xx responses
                    if (throwable instanceof WebClientResponseException) {
                        try {
                            return ((WebClientResponseException) throwable).getStatusCode().is5xxServerError();
                        } catch (Exception ex) {
                            return false;
                        }
                    }
                    // IO / connection exceptions
                    return (throwable instanceof IOException) || (throwable instanceof ConnectException);
                });
    }
}
