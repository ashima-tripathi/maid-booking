package com.example.maid.booking;

import com.example.maid.exception.InvalidRequestException;

import java.time.Duration;

final class Durations {

    static final int MIN_MINUTES = 30;
    static final int MAX_MINUTES = 8 * 60;

    private Durations() {
    }

    static Duration validated(Integer minutes) {
        if (minutes == null) {
            throw new InvalidRequestException("Duration is required");
        }
        if (minutes < MIN_MINUTES || minutes > MAX_MINUTES) {
            throw new InvalidRequestException(
                    "Duration must be between " + MIN_MINUTES + " and " + MAX_MINUTES + " minutes");
        }
        return Duration.ofMinutes(minutes);
    }
}
