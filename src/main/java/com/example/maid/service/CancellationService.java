package com.example.maid.service;

import com.example.maid.cancellation.CancellationPolicy;
import com.example.maid.cancellation.CancellationScope;
import com.example.maid.cancellation.CancelledOccurrence;
import com.example.maid.cancellation.RefundDecision;
import com.example.maid.domain.Booking;
import com.example.maid.domain.Money;
import com.example.maid.domain.Occurrence;
import com.example.maid.domain.OccurrenceStatus;
import com.example.maid.exception.InvalidRequestException;
import com.example.maid.exception.InvalidStateTransitionException;
import com.example.maid.exception.NotFoundException;
import com.example.maid.repository.BookingRepository;
import com.example.maid.repository.ScheduleRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public class CancellationService {

    private final BookingRepository bookingRepository;
    private final ScheduleRepository scheduleRepository;
    private final PaymentService paymentService;
    private final CancellationPolicy policy;
    private final Clock clock;

    public CancellationService(BookingRepository bookingRepository, ScheduleRepository scheduleRepository,
                               PaymentService paymentService, CancellationPolicy policy, Clock clock) {
        this.bookingRepository = bookingRepository;
        this.scheduleRepository = scheduleRepository;
        this.paymentService = paymentService;
        this.policy = policy;
        this.clock = clock;
    }

    /**
     * OCCURRENCE cancels exactly one visit (and fails if it cannot be cancelled).
     * SERIES cancels every visit that is still upcoming and leaves past or already-finished ones untouched.
     */
    public List<CancelledOccurrence> cancel(String bookingId, String occurrenceId, CancellationScope scope) {
        if (scope == null) {
            throw new InvalidRequestException("Cancellation scope is required");
        }
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found: " + bookingId));

        List<Occurrence> targets = switch (scope) {
            case OCCURRENCE -> {
                if (occurrenceId == null) {
                    throw new InvalidRequestException("Occurrence id is required to cancel a single occurrence");
                }
                yield List.of(booking.findOccurrence(occurrenceId));
            }
            case SERIES -> booking.occurrences().stream().filter(this::isUpcomingAndActive).toList();
        };
        if (targets.isEmpty()) {
            throw new InvalidStateTransitionException("Nothing left to cancel on this booking");
        }
        return targets.stream().map(occurrence -> cancelOne(booking, occurrence)).toList();
    }

    private CancelledOccurrence cancelOne(Booking booking, Occurrence occurrence) {
        Instant now = clock.instant();
        if (!occurrence.slot().start().isAfter(now)) {
            throw new InvalidStateTransitionException("Cannot cancel an occurrence that has already started");
        }
        RefundDecision decision = policy.evaluate(occurrence, now);   // judged on notice at the moment of cancelling
        OccurrenceStatus previous = occurrence.cancel();              // the state machine rejects invalid transitions

        Money refund = Money.ZERO;
        if (previous == OccurrenceStatus.CONFIRMED) {
            refund = occurrence.price().percent(decision.percent());
            paymentService.refund(occurrence.id(), refund);
        }
        scheduleRepository.release(booking.maidId(), occurrence.slot());
        return new CancelledOccurrence(occurrence.id(), refund);
    }

    private boolean isUpcomingAndActive(Occurrence occurrence) {
        return occurrence.isActive() && occurrence.slot().start().isAfter(clock.instant());
    }
}
