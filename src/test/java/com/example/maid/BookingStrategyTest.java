package com.example.maid;

import com.example.maid.booking.BookingRequest;
import com.example.maid.booking.InstantBookingStrategy;
import com.example.maid.booking.RecurrenceRule;
import com.example.maid.booking.RecurringBookingStrategy;
import com.example.maid.booking.ScheduledBookingStrategy;
import com.example.maid.domain.BookingType;
import com.example.maid.domain.ServiceType;
import com.example.maid.domain.TimeRange;
import com.example.maid.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static com.example.maid.Fixtures.NOW;
import static com.example.maid.Fixtures.ZONE;
import static com.example.maid.Fixtures.at;
import static com.example.maid.Fixtures.clock;
import static com.example.maid.Fixtures.daysFromToday;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BookingStrategyTest {

    private BookingRequest request(BookingType type, Instant start, Integer minutes, RecurrenceRule rule) {
        return new BookingRequest("c1", null, "Indiranagar", ServiceType.CLEANING, type, start, minutes, rule);
    }

    // ---- instant ----

    @Test
    void instantBookingStartsNowAndLastsTheRequestedDuration() {
        List<TimeRange> slots = new InstantBookingStrategy()
                .resolveSlots(request(BookingType.INSTANT, null, 90, null), clock());
        assertEquals(1, slots.size());
        assertEquals(NOW, slots.get(0).start());
        assertEquals(Duration.ofMinutes(90), slots.get(0).duration());
    }

    // ---- scheduled ----

    @Test
    void scheduledBookingUsesTheRequestedSlot() {
        Instant start = at(daysFromToday(1), 9, 0);
        List<TimeRange> slots = new ScheduledBookingStrategy()
                .resolveSlots(request(BookingType.SCHEDULED, start, 120, null), clock());
        assertEquals(List.of(TimeRange.of(start, Duration.ofHours(2))), slots);
    }

    @Test
    void scheduledBookingRequiresAStartTime() {
        assertThrows(InvalidRequestException.class, () -> new ScheduledBookingStrategy()
                .resolveSlots(request(BookingType.SCHEDULED, null, 60, null), clock()));
    }

    @Test
    void scheduledBookingCannotBeInThePast() {
        assertThrows(InvalidRequestException.class, () -> new ScheduledBookingStrategy()
                .resolveSlots(request(BookingType.SCHEDULED, NOW.minusSeconds(60), 60, null), clock()));
        assertThrows(InvalidRequestException.class, () -> new ScheduledBookingStrategy()
                .resolveSlots(request(BookingType.SCHEDULED, NOW, 60, null), clock()));
    }

    @Test
    void durationMustBeBetweenThirtyMinutesAndEightHours() {
        ScheduledBookingStrategy strategy = new ScheduledBookingStrategy();
        Instant start = at(daysFromToday(1), 9, 0);
        assertThrows(InvalidRequestException.class,
                () -> strategy.resolveSlots(request(BookingType.SCHEDULED, start, 15, null), clock()));
        assertThrows(InvalidRequestException.class,
                () -> strategy.resolveSlots(request(BookingType.SCHEDULED, start, 9 * 60, null), clock()));
        assertThrows(InvalidRequestException.class,
                () -> strategy.resolveSlots(request(BookingType.SCHEDULED, start, null, null), clock()));
    }

    // ---- recurring ----

    @Test
    void weekdayRuleSkipsTheWeekend() {
        // Wed 2 Dec -> Tue 8 Dec: Wed, Thu, Fri, Mon, Tue
        RecurrenceRule rule = RecurrenceRule.weekdays(LocalDate.of(2026, 12, 8));
        List<TimeRange> slots = new RecurringBookingStrategy()
                .resolveSlots(request(BookingType.RECURRING, at(daysFromToday(1), 9, 0), 60, rule), clock());

        assertEquals(5, slots.size());
        List<DayOfWeek> days = slots.stream().map(s -> s.start().atZone(ZONE).getDayOfWeek()).toList();
        assertEquals(List.of(DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY,
                DayOfWeek.MONDAY, DayOfWeek.TUESDAY), days);
    }

    @Test
    void everySlotKeepsTheSameLocalTimeOfDay() {
        RecurrenceRule rule = RecurrenceRule.weekdays(LocalDate.of(2026, 12, 8));
        List<TimeRange> slots = new RecurringBookingStrategy()
                .resolveSlots(request(BookingType.RECURRING, at(daysFromToday(1), 9, 0), 60, rule), clock());
        assertEquals(true, slots.stream().allMatch(s -> s.start().atZone(ZONE).getHour() == 9));
    }

    @Test
    void weeklyRuleProducesOneSlotPerWeek() {
        // Sundays: 6, 13, 20, 27 December
        RecurrenceRule rule = RecurrenceRule.weekly(DayOfWeek.SUNDAY, LocalDate.of(2026, 12, 27));
        List<TimeRange> slots = new RecurringBookingStrategy()
                .resolveSlots(request(BookingType.RECURRING, at(LocalDate.of(2026, 12, 6), 8, 0), 60, rule), clock());
        assertEquals(4, slots.size());
    }

    @Test
    void recurrenceCannotExceedTheMaximumHorizon() {
        RecurrenceRule tooFar = RecurrenceRule.weekdays(daysFromToday(1 + RecurrenceRule.MAX_HORIZON_DAYS + 1));
        assertThrows(InvalidRequestException.class, () -> new RecurringBookingStrategy()
                .resolveSlots(request(BookingType.RECURRING, at(daysFromToday(1), 9, 0), 60, tooFar), clock()));
    }

    @Test
    void recurrenceEndingBeforeItStartsIsRejected() {
        RecurrenceRule rule = RecurrenceRule.weekdays(TodayMinus());
        assertThrows(InvalidRequestException.class, () -> new RecurringBookingStrategy()
                .resolveSlots(request(BookingType.RECURRING, at(daysFromToday(1), 9, 0), 60, rule), clock()));
    }

    @Test
    void ruleThatMatchesNoDayIsRejected() {
        // start Wed 2 Dec, only Mondays, ends Thu 3 Dec -> zero occurrences
        RecurrenceRule rule = RecurrenceRule.weekly(DayOfWeek.MONDAY, LocalDate.of(2026, 12, 3));
        assertThrows(InvalidRequestException.class, () -> new RecurringBookingStrategy()
                .resolveSlots(request(BookingType.RECURRING, at(daysFromToday(1), 9, 0), 60, rule), clock()));
    }

    @Test
    void recurringBookingNeedsARule() {
        assertThrows(InvalidRequestException.class, () -> new RecurringBookingStrategy()
                .resolveSlots(request(BookingType.RECURRING, at(daysFromToday(1), 9, 0), 60, null), clock()));
    }

    private static LocalDate TodayMinus() {
        return Fixtures.TODAY;   // before the 2 Dec first slot
    }
}
