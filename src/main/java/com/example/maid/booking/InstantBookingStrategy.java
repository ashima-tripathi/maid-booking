package com.example.maid.booking;

import com.example.maid.domain.BookingType;
import com.example.maid.domain.TimeRange;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** Book a maid starting right now. */
public class InstantBookingStrategy implements BookingStrategy {

    @Override
    public BookingType type() {
        return BookingType.INSTANT;
    }

    @Override
    public List<TimeRange> resolveSlots(BookingRequest request, Clock clock) {
        Instant start = clock.instant().truncatedTo(ChronoUnit.MINUTES);
        return List.of(TimeRange.of(start, Durations.validated(request.durationMinutes())));
    }
}
