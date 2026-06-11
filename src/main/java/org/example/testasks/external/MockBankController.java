package org.example.testasks.external;

import lombok.RequiredArgsConstructor;
import org.example.testasks.external.dto.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/mock/api")
@RequiredArgsConstructor
public class MockBankController {

    private final MockBankDataService dataService;

    @GetMapping("/accounts/{iban}/balance")
    public ResponseEntity<ExternalBalanceDto> getBalance(
            @PathVariable String iban,
            @RequestHeader(value = "Authorization", required = false)
            String authHeader) {

        var account = dataService.getAccount(iban);
        if (!isAuthorized(authHeader)) {
            return ResponseEntity.status(401).build();
        }

        if (account == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(
                new ExternalBalanceDto(
                        account.getIban(),
                        account.getName(),
                        account.getBalance(),
                        account.getCurrency()
                )
        );
    }
    private boolean isAuthorized(String authHeader) {
        return "Bearer mock-token".equals(authHeader);
    }

    @GetMapping("/accounts/{iban}/transactions")
    public ResponseEntity<List<ExternalTransactionDto>> getTransactions(
            @PathVariable String iban,
            @RequestHeader(value = "Authorization", required = false)
            String authHeader) {
        if (!isAuthorized(authHeader)) {
            return ResponseEntity.status(401).build();
        }
        var account = dataService.getAccount(iban);

        if (account == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(
                account.getTransactions()
        );
    }

    @PostMapping("/payments")
    public ResponseEntity<PaymentResponseDto> initiatePayment(
            @RequestBody PaymentRequestDto request,
            @RequestHeader(value = "Authorization", required = false)
            String authHeader) {
        if (!isAuthorized(authHeader)) {
            return ResponseEntity.status(401).build();
        }

        String txId = dataService.initiatePayment(
                request.getFromIban(),
                request.getToIban(),
                request.getAmount(),
                request.getCurrency()
        );

        return ResponseEntity.ok(
                new PaymentResponseDto(
                        "COMPLETED",
                        txId
                )
        );
    }

    @PostMapping("/oauth/token")
    public OAuthTokenResponse token() {

        OAuthTokenResponse response = new OAuthTokenResponse();

        response.setAccessToken("mock-token");
        response.setTokenType("Bearer");
        response.setExpiresIn(3600L);

        return response;
    }
}