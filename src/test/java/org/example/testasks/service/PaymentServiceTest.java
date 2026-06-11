package org.example.testasks.service;

import org.example.testasks.api.dto.PaymentInitiateRequest;
import org.example.testasks.api.dto.PaymentInitiateResponse;
import org.example.testasks.exception.BadRequestException;
import org.example.testasks.exception.ConflictException;
import org.example.testasks.external.ExternalBankClient;
import org.example.testasks.external.dto.ExternalBalanceDto;
import org.example.testasks.external.dto.PaymentResponseDto;
import org.example.testasks.model.Payment;
import org.example.testasks.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    PaymentRepository paymentRepository;

    @Mock
    ExternalBankClient externalBankClient;

    @InjectMocks
    PaymentService paymentService;

    // ── Helpers ───────────────────────────────────────────────────────────

    private PaymentInitiateRequest buildRequest(String from, String to, double amount) {
        PaymentInitiateRequest req = new PaymentInitiateRequest();
        req.setFromIban(from);
        req.setToIban(to);
        req.setAmount(BigDecimal.valueOf(amount));
        req.setCurrency("EUR");
        return req;
    }

    /**
     * Configures paymentRepository.save() to assign sequential IDs:
     * first save → id=1, subsequent saves → same object returned.
     */
    private void stubRepoSave() {
        when(paymentRepository.save(any())).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            if (p.getId() == null) p.setId(1L);
            return p;
        });
        when(paymentRepository.findById(1L)).thenAnswer(inv ->
                paymentRepository.save(new Payment()) != null
                        ? Optional.of(buildSavedPayment())
                        : Optional.empty()
        );
    }

    private Payment buildSavedPayment() {
        Payment p = new Payment();
        p.setId(1L);
        p.setFromIban("UA893220010000026005000000001");
        p.setToIban("UA893220010000026005000000002");
        p.setAmount(BigDecimal.valueOf(100));
        p.setCurrency("EUR");
        p.setStatus(Payment.Status.SENT);
        return p;
    }

    // ── Tests ─────────────────────────────────────────────────────────────

    @Test
    void initiatePayment_success_returnsCompleted() {
        PaymentInitiateRequest req = buildRequest(
                "UA893220010000026005000000001",
                "UA893220010000026005000000002",
                100.0);

        when(externalBankClient.getBalance("UA893220010000026005000000001"))
                .thenReturn(new ExternalBalanceDto("UA893220010000026005000000001", "Ivanna", BigDecimal.valueOf(1000), "EUR"));
        when(externalBankClient.initiatePayment(any()))
                .thenReturn(new PaymentResponseDto("COMPLETED", "ref-123"));

        when(paymentRepository.save(any())).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            if (p.getId() == null) p.setId(1L);
            return p;
        });
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(buildSavedPayment()));

        PaymentInitiateResponse resp = paymentService.initiatePayment(req);

        assertNotNull(resp);
        assertNotNull(resp.getId());
        assertEquals("COMPLETED", resp.getStatus());
        assertEquals("ref-123", resp.getExternalReference());
    }

    @Test
    void initiatePayment_insufficientFunds_throwsConflict() {
        PaymentInitiateRequest req = buildRequest(
                "UA893220010000026005000000001",
                "UA893220010000026005000000002",
                5000.0);

        when(externalBankClient.getBalance("UA893220010000026005000000001"))
                .thenReturn(new ExternalBalanceDto("UA893220010000026005000000001", "Ivanna", BigDecimal.valueOf(100), "EUR"));

        assertThrows(ConflictException.class, () -> paymentService.initiatePayment(req));
        verifyNoInteractions(paymentRepository);
    }

    @Test
    void initiatePayment_sameIban_throwsBadRequest() {
        PaymentInitiateRequest req = buildRequest(
                "UA893220010000026005000000001",
                "UA893220010000026005000000001",
                100.0);

        assertThrows(BadRequestException.class, () -> paymentService.initiatePayment(req));
        verifyNoInteractions(paymentRepository);
        verifyNoInteractions(externalBankClient);
    }

    @Test
    void initiatePayment_zeroAmount_throwsBadRequest() {
        PaymentInitiateRequest req = buildRequest(
                "UA893220010000026005000000001",
                "UA893220010000026005000000002",
                0.0);

        assertThrows(BadRequestException.class, () -> paymentService.initiatePayment(req));
        verifyNoInteractions(paymentRepository);
        verifyNoInteractions(externalBankClient);
    }

    @Test
    void initiatePayment_externalReturnsFailed_statusIsFailed() {
        PaymentInitiateRequest req = buildRequest(
                "UA893220010000026005000000001",
                "UA893220010000026005000000002",
                100.0);

        when(externalBankClient.getBalance(any()))
                .thenReturn(new ExternalBalanceDto("UA893220010000026005000000001", "Ivanna", BigDecimal.valueOf(1000), "EUR"));
        when(externalBankClient.initiatePayment(any()))
                .thenReturn(new PaymentResponseDto("FAILED", null));

        when(paymentRepository.save(any())).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            if (p.getId() == null) p.setId(1L);
            return p;
        });
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(buildSavedPayment()));

        PaymentInitiateResponse resp = paymentService.initiatePayment(req);

        assertEquals("FAILED", resp.getStatus());
    }

    @Test
    void initiatePayment_exactBalance_succeeds() {
        PaymentInitiateRequest req = buildRequest(
                "UA893220010000026005000000001",
                "UA893220010000026005000000002",
                500.0);

        when(externalBankClient.getBalance(any()))
                .thenReturn(new ExternalBalanceDto("UA893220010000026005000000001", "Ivanna", BigDecimal.valueOf(500), "EUR"));
        when(externalBankClient.initiatePayment(any()))
                .thenReturn(new PaymentResponseDto("COMPLETED", "ref-exact"));

        when(paymentRepository.save(any())).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            if (p.getId() == null) p.setId(1L);
            return p;
        });
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(buildSavedPayment()));

        PaymentInitiateResponse resp = paymentService.initiatePayment(req);

        assertEquals("COMPLETED", resp.getStatus());
    }
}