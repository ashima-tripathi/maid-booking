package com.example.maid.booking;

import com.example.maid.domain.BookingType;
import com.example.maid.domain.TimeRange;

import java.time.Clock;
import java.util.List;

/**
 * One booking type. A strategy only answers "which time slots does this request mean?" -
 * validating that the request is well-formed for its type. Reservation, pricing and persistence
 * are identical for every type, so a new type (e.g. a weekly package) is one new class.
 */
public interface BookingStrategy {

    BookingType type();

    List<TimeRange> resolveSlots(BookingRequest request, Clock clock);
}
