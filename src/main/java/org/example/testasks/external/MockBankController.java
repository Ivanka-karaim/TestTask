package org.example.testasks.external;

import lombok.RequiredArgsConstructor;
import org.example.testasks.external.dto.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/mock/api")
@RequiredArgsConstructor
public class MockBankController {

    private final MockBankDataService dataService;

    @GetMapping("/accounts/{iban}/balance")
    public ResponseEntity<ExternalBalanceDto> getBalance(
            @PathVariable String iban) {

        var account = dataService.getAccount(iban);

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

    @GetMapping("/accounts/{iban}/transactions")
    public ResponseEntity<List<ExternalTransactionDto>> getTransactions(
            @PathVariable String iban) {

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
            @RequestBody PaymentRequestDto request) {

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
}