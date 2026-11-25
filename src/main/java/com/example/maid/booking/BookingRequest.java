package com.example.maid.booking;

import com.example.maid.domain.BookingType;
import com.example.maid.domain.ServiceType;

import java.time.Instant;

/**
 * Input to BookingService. maidId is optional: when absent the best available maid in the locality is chosen.
 * start is ignored for INSTANT bookings and is the first occurrence for RECURRING ones.
 */
public record BookingRequest(String customerId, String maidId, String locality, ServiceType serviceType,
                             BookingType bookingType, Instant start, Integer durationMinutes,
                             RecurrenceRule recurrence) {
}
