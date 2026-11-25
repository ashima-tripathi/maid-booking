package com.example.maid.domain;

import com.example.maid.exception.InvalidRequestException;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/** Half-open interval [start, end). Back-to-back ranges do not overlap. */
public record TimeRange(Instant start, Instant end) {

    public TimeRange {
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(end, "end");
        if (!end.isAfter(start)) {
            throw new InvalidRequestException("End time must be after start time");
        }
    }

    public static TimeRange of(Instant start, Duration duration) {
        return new TimeRange(start, start.plus(duration));
    }

    public boolean overlaps(TimeRange other) {
        return start.isBefore(other.end) && other.start.isBefore(end);
    }

    public Duration duration() {
        return Duration.between(start, end);
    }
}
