package com.example.maid;

import com.example.maid.domain.TimeRange;
import com.example.maid.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimeRangeTest {

    private final Instant nine = Instant.parse("2026-12-02T09:00:00Z");

    @Test
    void overlappingRangesOverlap() {
        TimeRange a = TimeRange.of(nine, Duration.ofHours(2));
        TimeRange b = TimeRange.of(nine.plus(Duration.ofHours(1)), Duration.ofHours(2));
        assertTrue(a.overlaps(b));
        assertTrue(b.overlaps(a));
    }

    @Test
    void backToBackRangesDoNotOverlap() {
        TimeRange a = TimeRange.of(nine, Duration.ofHours(1));
        TimeRange b = TimeRange.of(nine.plus(Duration.ofHours(1)), Duration.ofHours(1));
        assertFalse(a.overlaps(b));
        assertFalse(b.overlaps(a));
    }

    @Test
    void containedRangeOverlaps() {
        TimeRange outer = TimeRange.of(nine, Duration.ofHours(4));
        TimeRange inner = TimeRange.of(nine.plus(Duration.ofHours(1)), Duration.ofMinutes(30));
        assertTrue(outer.overlaps(inner));
    }

    @Test
    void endMustBeAfterStart() {
        assertThrows(InvalidRequestException.class, () -> new TimeRange(nine, nine));
        assertThrows(InvalidRequestException.class, () -> new TimeRange(nine, nine.minusSeconds(1)));
    }

    @Test
    void durationIsEndMinusStart() {
        assertEquals(Duration.ofMinutes(90), TimeRange.of(nine, Duration.ofMinutes(90)).duration());
    }
}
