package com.example.maid;

import com.example.maid.domain.AvailabilityWindow;
import com.example.maid.domain.Gender;
import com.example.maid.domain.Maid;
import com.example.maid.domain.Money;
import com.example.maid.domain.ServiceType;
import com.example.maid.domain.TimeRange;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

/** Shared test data. "Now" is fixed at Tuesday 2026-12-01 10:00 IST so every time-based rule is deterministic. */
public final class Fixtures {

    public static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");
    public static final Instant NOW = Instant.parse("2026-12-01T04:30:00Z");
    public static final LocalDate TODAY = LocalDate.of(2026, 12, 1);

    private Fixtures() {
    }

    public static Clock clock() {
        return Clock.fixed(NOW, ZONE);
    }

    /** Working every day 08:00-20:00. */
    public static List<AvailabilityWindow> everyDay8to20() {
        return Arrays.stream(DayOfWeek.values())
                .map(day -> new AvailabilityWindow(day, LocalTime.of(8, 0), LocalTime.of(20, 0)))
                .toList();
    }

    public static Maid maid(String id, String locality, double rating, double hourlyRate, Gender gender,
                            ServiceType... skills) {
        return new Maid(id, "Maid " + id, locality, EnumSet.copyOf(Arrays.asList(skills)), Money.of(hourlyRate),
                rating, gender, everyDay8to20());
    }

    public static Maid cleaner(String id) {
        return maid(id, "Indiranagar", 4.5, 300, Gender.FEMALE, ServiceType.CLEANING);
    }

    public static Instant at(LocalDate date, int hour, int minute) {
        return date.atTime(hour, minute).atZone(ZONE).toInstant();
    }

    public static LocalDate daysFromToday(int days) {
        return TODAY.plusDays(days);
    }

    public static TimeRange slot(LocalDate date, int hour, int minute, int durationMinutes) {
        return TimeRange.of(at(date, hour, minute), Duration.ofMinutes(durationMinutes));
    }
}
