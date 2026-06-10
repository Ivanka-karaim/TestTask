package org.example.testasks.controller;

import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.example.testasks.api.dto.AccountBalanceResponse;
import org.example.testasks.api.dto.TransactionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import org.example.testasks.service.AccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
@Validated
public class AccountController {

    private final AccountService accountService;


    @GetMapping("/{iban}/balance")
    @Operation(summary = "Get account balance", description = "Returns current balance and currency for the specified IBAN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Balance returned", content = @Content(schema = @Schema(implementation = AccountBalanceResponse.class))),
            @ApiResponse(responseCode = "400", description = "Bad request or validation error")
    })
    public ResponseEntity<AccountBalanceResponse> getBalance(@PathVariable("iban") @Pattern(
            regexp = "^UA[0-9]{27}$",
            message = "Invalid IBAN format"
    ) String iban) {
        var resp = accountService.getBalance(iban);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/{iban}/transactions")
    @Operation(summary = "Get recent transactions", description = "Returns the most recent transactions (persisted from external bank) for the specified IBAN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of transactions returned", content = @Content(schema = @Schema(implementation = TransactionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Bad request or validation error")
    })
    public ResponseEntity<List<TransactionResponse>> getTransactions(@PathVariable("iban") @Pattern(
            regexp = "^UA[0-9]{27}$",
            message = "Invalid IBAN format"
    ) String iban) {
        var resp = accountService.getTransactions(iban);
        return ResponseEntity.ok(resp);
    }
}
