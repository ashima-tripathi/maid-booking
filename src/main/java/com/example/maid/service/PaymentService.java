package com.example.maid.service;

import com.example.maid.domain.Booking;
import com.example.maid.domain.Money;
import com.example.maid.domain.Occurrence;
import com.example.maid.domain.OccurrenceStatus;
import com.example.maid.domain.Payment;
import com.example.maid.exception.InvalidRequestException;
import com.example.maid.exception.InvalidStateTransitionException;
import com.example.maid.exception.NotFoundException;
import com.example.maid.exception.PaymentFailedException;
import com.example.maid.payment.GatewayResult;
import com.example.maid.payment.PaymentProcessor;
import com.example.maid.payment.PaymentProcessorRegistry;
import com.example.maid.payment.PaymentRequest;
import com.example.maid.repository.BookingRepository;
import com.example.maid.repository.PaymentRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Payment is taken per occurrence, so a recurring booking is paid one visit at a time and each visit can
 * be refunded on its own when cancelled.
 */
public class PaymentService {

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentProcessorRegistry processors;

    public PaymentService(BookingRepository bookingRepository, PaymentRepository paymentRepository,
                          PaymentProcessorRegistry processors) {
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.processors = processors;
    }

    /** Idempotent: a retry with the same key returns the original payment and never charges twice. */
    public Payment pay(PaymentRequest request) {
        if (request.idempotencyKey() == null || request.idempotencyKey().isBlank()) {
            throw new InvalidRequestException("Idempotency key is required");
        }
        Optional<Payment> replay = findReplay(request);
        if (replay.isPresent()) {
            return replay.get();
        }

        Booking booking = bookingRepository.findById(request.bookingId())
                .orElseThrow(() -> new NotFoundException("Booking not found: " + request.bookingId()));
        Occurrence occurrence = booking.findOccurrence(request.occurrenceId());
        PaymentProcessor processor = processors.forMethod(request.method());

        synchronized (occurrence) {   // serialises concurrent attempts to pay the same occurrence
            replay = findReplay(request);
            if (replay.isPresent()) {
                return replay.get();
            }
            if (occurrence.status() != OccurrenceStatus.PENDING_PAYMENT) {
                throw new InvalidStateTransitionException(
                        "Occurrence is " + occurrence.status() + " and cannot be paid for");
            }
            GatewayResult result = processor.charge(occurrence.price(), request.instrument(), request.idempotencyKey());
            if (!result.success()) {
                throw new PaymentFailedException(result.message());
            }
            occurrence.confirmPayment();
            return paymentRepository.save(new Payment(UUID.randomUUID().toString(), occurrence.id(),
                    occurrence.price(), request.method(), request.idempotencyKey(), result.transactionRef()));
        }
    }

    /** Refunds part or all of an occurrence's payment. No-op when nothing was paid or the amount is zero. */
    public void refund(String occurrenceId, Money amount) {
        if (amount.isZero()) {
            return;
        }
        paymentRepository.findByOccurrenceId(occurrenceId).ifPresent(payment -> {
            GatewayResult result = processors.forMethod(payment.method()).refund(payment.transactionRef(), amount);
            if (!result.success()) {
                throw new PaymentFailedException("Refund failed: " + result.message());
            }
            payment.markRefunded(amount);
        });
    }

    private Optional<Payment> findReplay(PaymentRequest request) {
        Optional<Payment> existing = paymentRepository.findByIdempotencyKey(request.idempotencyKey());
        existing.ifPresent(payment -> {
            if (!payment.occurrenceId().equals(request.occurrenceId())) {
                throw new InvalidRequestException("Idempotency key was already used for a different payment");
            }
        });
        return existing;
    }
}
