package com.example.maid.domain;

import com.example.maid.exception.InvalidStateTransitionException;

import java.util.Objects;

/**
 * One concrete, bookable slot. A one-off booking has a single occurrence; a recurring booking has many.
 * Lifecycle: PENDING_PAYMENT -> CONFIRMED -> COMPLETED, with CANCELLED reachable from the first two.
 * Transitions are synchronized so concurrent pay/cancel calls cannot both win.
 */
public class Occurrence {

    private final String id;
    private final TimeRange slot;
    private final Money price;
    private OccurrenceStatus status = OccurrenceStatus.PENDING_PAYMENT;

    public Occurrence(String id, TimeRange slot, Money price) {
        this.id = Objects.requireNonNull(id, "id");
        this.slot = Objects.requireNonNull(slot, "slot");
        this.price = Objects.requireNonNull(price, "price");
    }

    public synchronized void confirmPayment() {
        requireStatus(OccurrenceStatus.PENDING_PAYMENT, "confirm payment for");
        status = OccurrenceStatus.CONFIRMED;
    }

    public synchronized void complete() {
        requireStatus(OccurrenceStatus.CONFIRMED, "complete");
        status = OccurrenceStatus.COMPLETED;
    }

    /** Cancels the occurrence and returns the status it had, so callers know whether a refund applies. */
    public synchronized OccurrenceStatus cancel() {
        if (!isActive()) {
            throw new InvalidStateTransitionException("Cannot cancel an occurrence that is " + status);
        }
        OccurrenceStatus previous = status;
        status = OccurrenceStatus.CANCELLED;
        return previous;
    }

    public synchronized boolean isActive() {
        return status == OccurrenceStatus.PENDING_PAYMENT || status == OccurrenceStatus.CONFIRMED;
    }

    private void requireStatus(OccurrenceStatus expected, String action) {
        if (status != expected) {
            throw new InvalidStateTransitionException("Cannot " + action + " an occurrence that is " + status);
        }
    }

    public String id() { return id; }
    public TimeRange slot() { return slot; }
    public Money price() { return price; }
    public synchronized OccurrenceStatus status() { return status; }
}
