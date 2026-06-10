package org.example.testasks.service;

import org.example.testasks.api.dto.PaymentInitiateRequest;
import org.example.testasks.exception.ConflictException;
import org.example.testasks.external.ExternalBankClient;
import org.example.testasks.external.dto.ExternalBalanceDto;
import org.example.testasks.external.dto.PaymentResponseDto;
import org.example.testasks.model.Payment;
import org.example.testasks.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    PaymentRepository paymentRepository;

    @Mock
    ExternalBankClient externalBankClient;

    @InjectMocks
    PaymentService paymentService;

    @BeforeEach
    void setUp() {
    }

    @Test
    void initiatePayment_success() {
        PaymentInitiateRequest req = new PaymentInitiateRequest();
        req.setFromIban("DE123");
        req.setToIban("DE999");
        req.setAmount(BigDecimal.valueOf(100));
        req.setCurrency("EUR");

        when(externalBankClient.getBalance("DE123")).thenReturn(new ExternalBalanceDto("DE123","Ivanna", BigDecimal.valueOf(1000), "EUR"));
        when(externalBankClient.initiatePayment(any())).thenReturn(new PaymentResponseDto("COMPLETED", "ref-123"));

        when(paymentRepository.save(any())).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            if (p.getId() == null) p.setId(1L);
            return p;
        });

        var resp = paymentService.initiatePayment(req);

        assertNotNull(resp);
        assertEquals("COMPLETED", resp.getStatus());
        assertEquals("ref-123", resp.getExternalReference());
    }

    @Test
    void initiatePayment_insufficientFunds() {
        PaymentInitiateRequest req = new PaymentInitiateRequest();
        req.setFromIban("DE111");
        req.setToIban("DE999");
        req.setAmount(BigDecimal.valueOf(5000));
        req.setCurrency("EUR");

        when(externalBankClient.getBalance("DE111")).thenReturn(new ExternalBalanceDto("DE111", "Ivanna", BigDecimal.valueOf(100), "EUR"));

        assertThrows(ConflictException.class, () -> paymentService.initiatePayment(req));
    }
}
