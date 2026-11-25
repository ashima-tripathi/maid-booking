package com.example.maid.repository;

import com.example.maid.domain.Payment;

import java.util.Optional;

public interface PaymentRepository {
    Payment save(Payment payment);

    Optional<Payment> findByOccurrenceId(String occurrenceId);

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
}
