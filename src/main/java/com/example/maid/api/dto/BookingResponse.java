package com.example.maid.api.dto;

import com.example.maid.domain.Booking;
import com.example.maid.domain.BookingStatus;
import com.example.maid.domain.BookingType;
import com.example.maid.domain.Occurrence;
import com.example.maid.domain.OccurrenceStatus;
import com.example.maid.domain.ServiceType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record BookingResponse(String id, String customerId, String maidId, ServiceType serviceType,
                              BookingType bookingType, BookingStatus status, Instant createdAt,
                              List<OccurrenceResponse> occurrences) {

    public record OccurrenceResponse(String id, Instant start, Instant end, BigDecimal price,
                                     OccurrenceStatus status) {
        static OccurrenceResponse from(Occurrence occurrence) {
            return new OccurrenceResponse(occurrence.id(), occurrence.slot().start(), occurrence.slot().end(),
                    occurrence.price().amount(), occurrence.status());
        }
    }

    public static BookingResponse from(Booking booking) {
        return new BookingResponse(booking.id(), booking.customerId(), booking.maidId(), booking.serviceType(),
                booking.bookingType(), booking.status(), booking.createdAt(),
                booking.occurrences().stream().map(OccurrenceResponse::from).toList());
    }
}
