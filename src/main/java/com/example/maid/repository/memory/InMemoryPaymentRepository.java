package com.example.maid.repository.memory;

import com.example.maid.domain.Payment;
import com.example.maid.repository.PaymentRepository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryPaymentRepository implements PaymentRepository {

    private final Map<String, Payment> byOccurrence = new ConcurrentHashMap<>();
    private final Map<String, Payment> byIdempotencyKey = new ConcurrentHashMap<>();

    @Override
    public Payment save(Payment payment) {
        byOccurrence.put(payment.occurrenceId(), payment);
        byIdempotencyKey.put(payment.idempotencyKey(), payment);
        return payment;
    }

    @Override
    public Optional<Payment> findByOccurrenceId(String occurrenceId) {
        return Optional.ofNullable(byOccurrence.get(occurrenceId));
    }

    @Override
    public Optional<Payment> findByIdempotencyKey(String idempotencyKey) {
        return Optional.ofNullable(byIdempotencyKey.get(idempotencyKey));
    }
}
