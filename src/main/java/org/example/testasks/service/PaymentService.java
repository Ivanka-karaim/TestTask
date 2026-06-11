package org.example.testasks.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

/**
 * Service responsible for payment initiation workflow.
 *
 * <h3>Payment state machine:</h3>
 * <pre>
 *   PENDING → SENT → COMPLETED
 *                  ↘ FAILED
 * </pre>
 *
 * <h3>Why @Transactional is intentionally NOT on the main method:</h3>
 * <p>
 * The external HTTP call must NOT be wrapped in a DB transaction.
 * If it were, a rollback (e.g. DB error after a successful external call)
 * would erase the DB record — but the external payment would already be
 * executed, leaving the two systems permanently out of sync.
 * </p>
 * <p>
 * Instead, the flow is split into three separate transactional steps:
 * </p>
 * <ol>
 *   <li>{@link #createPendingPayment} — saves PENDING inside a committed transaction</li>
 *   <li>External HTTP call — happens outside any transaction</li>
 *   <li>{@link #finalizePayment} — saves COMPLETED/FAILED inside a new committed transaction</li>
 * </ol>
 * <p>
 * If the JVM crashes between steps 1 and 3, the payment stays in SENT state
 * and can be recovered by a scheduled job.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final ExternalBankClient externalBankClient;

    /**
     * Initiates a payment.
     *
     * <p>This method is intentionally NOT annotated with @Transactional —
     * see class-level Javadoc for explanation.</p>
     *
     * @throws BadRequestException if amount is invalid or IBANs are equal
     * @throws ConflictException   if the source account has insufficient funds
     */
    public PaymentInitiateResponse initiatePayment(PaymentInitiateRequest req) {
        validateRequest(req);

        var balance = externalBankClient.getBalance(req.getFromIban());
        if (balance.getBalance().compareTo(req.getAmount()) < 0) {
            throw new ConflictException("Insufficient funds");
        }

        // Step 1: persist PENDING — committed immediately (own transaction)
        Payment payment = createPendingPayment(req);
        log.info("Payment {} created with status PENDING", payment.getId());

        // Step 2: mark as SENT before calling external API.
        // If the JVM crashes here, the record stays SENT and can be recovered.
        markAsSent(payment.getId());
        log.info("Payment {} marked as SENT", payment.getId());

        // Step 3: call external bank — outside any DB transaction
        PaymentResponseDto externalResponse = callExternalBank(payment);

        // Step 4: persist final status — committed immediately (own transaction)
        Payment finalized = finalizePayment(payment.getId(), externalResponse);
        log.info("Payment {} finalized with status {}", finalized.getId(), finalized.getStatus());

        return new PaymentInitiateResponse(
                finalized.getId(),
                finalized.getStatus().name(),
                finalized.getExternalReference()
        );
    }

    /**
     * Persists a new payment with status PENDING.
     * Runs in its own transaction — commits immediately on return.
     */
    @Transactional
    public Payment createPendingPayment(PaymentInitiateRequest req) {
        Payment payment = Payment.builder()
                .fromIban(req.getFromIban())
                .toIban(req.getToIban())
                .amount(req.getAmount())
                .currency(req.getCurrency())
                .status(Payment.Status.PENDING)
                .createdAt(OffsetDateTime.now())
                .build();
        return paymentRepository.save(payment);
    }

    /**
     * Transitions payment to SENT.
     * Runs in its own transaction — commits immediately on return.
     * Having SENT committed to DB before the external call ensures
     * the record is recoverable even if the process crashes mid-flight.
     */
    @Transactional
    public void markAsSent(Long paymentId) {
        Payment payment = getPaymentOrThrow(paymentId);
        payment.setStatus(Payment.Status.SENT);
        payment.setUpdatedAt(OffsetDateTime.now());
        paymentRepository.save(payment);
    }

    /**
     * Calls the external bank API.
     * On failure, marks the payment as FAILED and rethrows the exception.
     * This method is NOT transactional — DB updates happen in finalizePayment().
     */
    private PaymentResponseDto callExternalBank(Payment payment) {
        try {
            return externalBankClient.initiatePayment(
                    new PaymentRequestDto(
                            payment.getFromIban(),
                            payment.getToIban(),
                            payment.getAmount(),
                            payment.getCurrency()
                    )
            );
        } catch (Exception ex) {
            log.error("External bank call failed for payment {}: {}", payment.getId(), ex.getMessage());
            finalizePayment(payment.getId(), null);
            throw ex;
        }
    }

    /**
     * Persists the final payment status based on the external bank's response.
     * Runs in its own transaction — commits immediately on return.
     *
     * @param externalResponse null if the external call failed entirely
     */
    @Transactional
    public Payment finalizePayment(Long paymentId, PaymentResponseDto externalResponse) {
        Payment payment = getPaymentOrThrow(paymentId);

        boolean completed = externalResponse != null
                && "COMPLETED".equalsIgnoreCase(externalResponse.getStatus());

        payment.setStatus(completed ? Payment.Status.COMPLETED : Payment.Status.FAILED);
        payment.setUpdatedAt(OffsetDateTime.now());

        if (externalResponse != null) {
            payment.setExternalReference(externalResponse.getExternalReference());
        }

        return paymentRepository.save(payment);
    }

    // ── Private helpers ───────────────────────────────────────────────────

    private void validateRequest(PaymentInitiateRequest req) {
        if (req.getAmount() == null || req.getAmount().signum() <= 0) {
            throw new BadRequestException("Invalid amount");
        }
        if (req.getFromIban().equals(req.getToIban())) {
            throw new BadRequestException("Source and destination IBAN must differ");
        }
    }

    private Payment getPaymentOrThrow(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalStateException(
                        "Payment not found: " + paymentId));
    }
}