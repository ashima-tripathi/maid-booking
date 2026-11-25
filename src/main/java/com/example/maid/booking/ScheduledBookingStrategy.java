package com.example.maid.booking;

import com.example.maid.domain.BookingType;
import com.example.maid.domain.TimeRange;
import com.example.maid.exception.InvalidRequestException;

import java.time.Clock;
import java.util.List;

/** Book one specific future slot. */
public class ScheduledBookingStrategy implements BookingStrategy {

    @Override
    public BookingType type() {
        return BookingType.SCHEDULED;
    }

    @Override
    public List<TimeRange> resolveSlots(BookingRequest request, Clock clock) {
        if (request.start() == null) {
            throw new InvalidRequestException("Start time is required for a scheduled booking");
        }
        if (!request.start().isAfter(clock.instant())) {
            throw new InvalidRequestException("Scheduled start time must be in the future");
        }
        return List.of(TimeRange.of(request.start(), Durations.validated(request.durationMinutes())));
    }
}
