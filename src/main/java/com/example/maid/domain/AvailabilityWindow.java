package com.example.maid.domain;

import com.example.maid.exception.InvalidRequestException;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;

/** A recurring weekly working window, e.g. every Monday 09:00-17:00. Windows do not span midnight. */
public record AvailabilityWindow(DayOfWeek day, LocalTime from, LocalTime to) {

    public AvailabilityWindow {
        Objects.requireNonNull(day, "day");
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        if (!to.isAfter(from)) {
            throw new InvalidRequestException("Availability window must end after it starts");
        }
    }

    public boolean covers(TimeRange range, ZoneId zone) {
        ZonedDateTime start = range.start().atZone(zone);
        ZonedDateTime end = range.end().atZone(zone);
        return start.getDayOfWeek() == day
                && start.toLocalDate().equals(end.toLocalDate())
                && !start.toLocalTime().isBefore(from)
                && !end.toLocalTime().isAfter(to);
    }
}
