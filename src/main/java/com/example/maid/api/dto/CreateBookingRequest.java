package com.example.maid.api.dto;

import com.example.maid.booking.BookingRequest;
import com.example.maid.booking.RecurrenceRule;
import com.example.maid.domain.BookingType;
import com.example.maid.domain.ServiceType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

public record CreateBookingRequest(
        @NotBlank String customerId,
        String maidId,
        String locality,
        @NotNull ServiceType serviceType,
        @NotNull BookingType bookingType,
        Instant start,
        @NotNull Integer durationMinutes,
        @Valid RecurrenceDto recurrence) {

    public record RecurrenceDto(@NotNull Set<DayOfWeek> days, @NotNull LocalDate until) {
    }

    public BookingRequest toDomain() {
        RecurrenceRule rule = recurrence == null ? null : new RecurrenceRule(recurrence.days(), recurrence.until());
        return new BookingRequest(customerId, maidId, locality, serviceType, bookingType, start,
                durationMinutes, rule);
    }
}
