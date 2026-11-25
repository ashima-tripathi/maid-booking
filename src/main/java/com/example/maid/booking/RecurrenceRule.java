package com.example.maid.booking;

import com.example.maid.domain.TimeRange;
import com.example.maid.exception.InvalidRequestException;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * "These weekdays, at the first slot's time of day, until this date (inclusive)".
 * Bounded to MAX_HORIZON_DAYS so a series cannot lock a maid's calendar indefinitely.
 */
public record RecurrenceRule(Set<DayOfWeek> days, LocalDate until) {

    public static final int MAX_HORIZON_DAYS = 84;

    public RecurrenceRule {
        if (days == null || days.isEmpty()) {
            throw new InvalidRequestException("Recurrence needs at least one day of the week");
        }
        if (until == null) {
            throw new InvalidRequestException("Recurrence needs an end date");
        }
        days = Set.copyOf(EnumSet.copyOf(days));
    }

    public static RecurrenceRule weekdays(LocalDate until) {
        return new RecurrenceRule(EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY), until);
    }

    public static RecurrenceRule weekly(DayOfWeek day, LocalDate until) {
        return new RecurrenceRule(EnumSet.of(day), until);
    }

    /** Expands the rule into concrete slots, starting from the date of firstStart. */
    public List<TimeRange> slots(Instant firstStart, Duration duration, ZoneId zone) {
        ZonedDateTime first = firstStart.atZone(zone);
        LocalDate startDate = first.toLocalDate();
        if (until.isBefore(startDate)) {
            throw new InvalidRequestException("Recurrence end date is before the first slot");
        }
        if (until.isAfter(startDate.plusDays(MAX_HORIZON_DAYS))) {
            throw new InvalidRequestException("Recurring bookings are limited to " + MAX_HORIZON_DAYS + " days");
        }
        LocalTime timeOfDay = first.toLocalTime();
        List<TimeRange> slots = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(until); date = date.plusDays(1)) {
            if (days.contains(date.getDayOfWeek())) {
                Instant start = date.atTime(timeOfDay).atZone(zone).toInstant();
                slots.add(TimeRange.of(start, duration));
            }
        }
        return slots;
    }
}
