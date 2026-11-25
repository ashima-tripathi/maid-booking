package com.example.maid.booking;

import com.example.maid.domain.BookingType;
import com.example.maid.domain.TimeRange;
import com.example.maid.exception.InvalidRequestException;

import java.time.Clock;
import java.util.List;

/** Book a repeating slot, e.g. every weekday at 9am or every Sunday. */
public class RecurringBookingStrategy implements BookingStrategy {

    @Override
    public BookingType type() {
        return BookingType.RECURRING;
    }

    @Override
    public List<TimeRange> resolveSlots(BookingRequest request, Clock clock) {
        if (request.start() == null) {
            throw new InvalidRequestException("Start time is required for a recurring booking");
        }
        if (request.recurrence() == null) {
            throw new InvalidRequestException("Recurrence rule is required for a recurring booking");
        }
        if (!request.start().isAfter(clock.instant())) {
            throw new InvalidRequestException("Recurring start time must be in the future");
        }
        List<TimeRange> slots = request.recurrence()
                .slots(request.start(), Durations.validated(request.durationMinutes()), clock.getZone());
        if (slots.isEmpty()) {
            throw new InvalidRequestException("Recurrence rule produces no occurrences");
        }
        return slots;
    }
}
