package org.example.testasks.service;

import lombok.RequiredArgsConstructor;
import org.example.testasks.api.dto.PaymentInitiateRequest;
import org.example.testasks.api.dto.PaymentInitiateResponse;
import org.example.testasks.exception.BadRequestException;
import org.example.testasks.exception.ConflictException;
import org.example.testasks.external.ExternalBankClient;
import org.example.testasks.external.dto.PaymentRequestDto;
import org.example.testasks.external.dto.PaymentResponseDto;
import org.example.testasks.model.Payment;
import org.example.testasks.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final ExternalBankClient externalBankClient;


    @Transactional
    public PaymentInitiateResponse initiatePayment(PaymentInitiateRequest req) {
        if (req.getAmount() == null || req.getAmount().signum() <= 0) {
            throw new BadRequestException("Invalid amount");
        }

        var balance = externalBankClient.getBalance(req.getFromIban());
        if (balance.getBalance().compareTo(req.getAmount()) < 0) {
            throw new ConflictException("Insufficient funds");
        }

        Payment p = Payment.builder()
                .fromIban(req.getFromIban())
                .toIban(req.getToIban())
                .amount(req.getAmount())
                .currency(req.getCurrency())
                .status(Payment.Status.PENDING)
                .createdAt(OffsetDateTime.now())
                .build();

        p = paymentRepository.save(p);

        PaymentRequestDto outReq = new PaymentRequestDto(req.getFromIban(), req.getToIban(), req.getAmount(), req.getCurrency());
        PaymentResponseDto resp = externalBankClient.initiatePayment(outReq);

        if (resp != null && "COMPLETED".equalsIgnoreCase(resp.getStatus())) {
            p.setStatus(Payment.Status.COMPLETED);
        } else {
            p.setStatus(Payment.Status.FAILED);
        }
        if (resp != null) p.setExternalReference(resp.getExternalReference());
        p.setUpdatedAt(OffsetDateTime.now());
        p = paymentRepository.save(p);

        return new PaymentInitiateResponse( p.getStatus().name(), p.getExternalReference());
    }
}
