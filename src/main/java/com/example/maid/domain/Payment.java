package com.example.maid.domain;

import com.example.maid.exception.InvalidStateTransitionException;

import java.util.Objects;

/** The payment taken for a single occurrence. */
public class Payment {

    private final String id;
    private final String occurrenceId;
    private final Money amount;
    private final PaymentMethodType method;
    private final String idempotencyKey;
    private final String transactionRef;
    private PaymentStatus status = PaymentStatus.PAID;
    private Money refunded = Money.ZERO;

    public Payment(String id, String occurrenceId, Money amount, PaymentMethodType method,
                   String idempotencyKey, String transactionRef) {
        this.id = Objects.requireNonNull(id, "id");
        this.occurrenceId = Objects.requireNonNull(occurrenceId, "occurrenceId");
        this.amount = Objects.requireNonNull(amount, "amount");
        this.method = Objects.requireNonNull(method, "method");
        this.idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey");
        this.transactionRef = Objects.requireNonNull(transactionRef, "transactionRef");
    }

    public synchronized void markRefunded(Money refundAmount) {
        if (status != PaymentStatus.PAID) {
            throw new InvalidStateTransitionException("Payment is already " + status);
        }
        if (refundAmount.compareTo(amount) > 0) {
            throw new InvalidStateTransitionException("Refund cannot exceed the amount paid");
        }
        refunded = refundAmount;
        status = refundAmount.compareTo(amount) == 0 ? PaymentStatus.REFUNDED : PaymentStatus.PARTIALLY_REFUNDED;
    }

    public String id() { return id; }
    public String occurrenceId() { return occurrenceId; }
    public Money amount() { return amount; }
    public PaymentMethodType method() { return method; }
    public String idempotencyKey() { return idempotencyKey; }
    public String transactionRef() { return transactionRef; }
    public synchronized PaymentStatus status() { return status; }
    public synchronized Money refunded() { return refunded; }
}
