package org.example.testasks.controller;

import org.example.testasks.api.dto.PaymentInitiateRequest;
import org.example.testasks.api.dto.PaymentInitiateResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import org.example.testasks.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/initiate")
    @Operation(summary = "Initiate payment", description = "Initiates an IBAN-to-IBAN payment. Saves payment locally, validates balance, then sends to external bank.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment accepted/processed", content = @Content(schema = @Schema(implementation = PaymentInitiateResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error or bad request"),
            @ApiResponse(responseCode = "409", description = "Conflict, e.g., insufficient funds")
    })
    public ResponseEntity<PaymentInitiateResponse> initiate(@jakarta.validation.Valid @RequestBody PaymentInitiateRequest req) {
        var resp = paymentService.initiatePayment(req);
        return ResponseEntity.ok(resp);
    }
}
