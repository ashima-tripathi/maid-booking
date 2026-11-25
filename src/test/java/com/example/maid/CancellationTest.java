package com.example.maid;

import com.example.maid.booking.RecurrenceRule;
import com.example.maid.cancellation.CancellationScope;
import com.example.maid.cancellation.CancelledOccurrence;
import com.example.maid.cancellation.RefundDecision;
import com.example.maid.cancellation.TimeBasedCancellationPolicy;
import com.example.maid.domain.Booking;
import com.example.maid.domain.BookingStatus;
import com.example.maid.domain.Money;
import com.example.maid.domain.Occurrence;
import com.example.maid.domain.OccurrenceStatus;
import com.example.maid.domain.PaymentStatus;
import com.example.maid.exception.InvalidRequestException;
import com.example.maid.exception.InvalidStateTransitionException;
import com.example.maid.exception.NotFoundException;
import com.example.maid.exception.SlotUnavailableException;
import com.example.maid.search.SearchCriteria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static com.example.maid.Fixtures.NOW;
import static com.example.maid.Fixtures.at;
import static com.example.maid.Fixtures.daysFromToday;
import static com.example.maid.Fixtures.slot;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CancellationTest {

    private final TimeBasedCancellationPolicy policy = new TimeBasedCancellationPolicy();
    private Wiring app;

    @BeforeEach
    void setUp() {
        app = new Wiring();
        app.addMaid(Fixtures.cleaner("asha"));   // 300/hour
    }

    private Booking paidBooking(Instant start, int minutes) {
        Booking booking = app.bookScheduled("asha", start, minutes);
        app.paymentService.pay(app.cardPayment(booking, 0, "key-" + booking.id()));
        return booking;
    }

    private Occurrence occurrenceStartingIn(Duration notice) {
        return new Occurrence("o", com.example.maid.domain.TimeRange.of(NOW.plus(notice), Duration.ofHours(1)), Money.of(100));
    }

    // ---- refund policy ----

    @Test
    void policyRefundsInFullWithAtLeast24HoursNotice() {
        assertEquals(new RefundDecision(100), policy.evaluate(occurrenceStartingIn(Duration.ofHours(24)), NOW));
        assertEquals(new RefundDecision(100), policy.evaluate(occurrenceStartingIn(Duration.ofDays(5)), NOW));
    }

    @Test
    void policyRefundsHalfBetween2And24Hours() {
        assertEquals(new RefundDecision(50), policy.evaluate(occurrenceStartingIn(Duration.ofHours(23)), NOW));
        assertEquals(new RefundDecision(50), policy.evaluate(occurrenceStartingIn(Duration.ofHours(2)), NOW));
    }

    @Test
    void policyRefundsNothingUnder2Hours() {
        assertEquals(new RefundDecision(0), policy.evaluate(occurrenceStartingIn(Duration.ofMinutes(119)), NOW));
    }

    @Test
    void refundPercentMustBeValid() {
        assertThrows(InvalidRequestException.class, () -> new RefundDecision(101));
        assertThrows(InvalidRequestException.class, () -> new RefundDecision(-1));
    }

    // ---- cancelling one occurrence ----

    @Test
    void cancellingWithPlentyOfNoticeRefundsEverything() {
        Booking booking = paidBooking(at(daysFromToday(3), 10, 0), 120);   // 600

        List<CancelledOccurrence> result = app.cancellationService.cancel(
                booking.id(), booking.occurrences().get(0).id(), CancellationScope.OCCURRENCE);

        assertEquals(Money.of(600), result.get(0).refund());
        assertEquals(OccurrenceStatus.CANCELLED, booking.occurrences().get(0).status());
        assertEquals(BookingStatus.CANCELLED, booking.status());
        assertEquals(PaymentStatus.REFUNDED, app.payments.findByOccurrenceId(booking.occurrences().get(0).id()).get().status());
    }

    @Test
    void cancellingLateRefundsHalf() {
        Booking booking = paidBooking(at(Fixtures.TODAY, 18, 0), 120);   // 8 hours from "now"

        List<CancelledOccurrence> result = app.cancellationService.cancel(
                booking.id(), booking.occurrences().get(0).id(), CancellationScope.OCCURRENCE);

        assertEquals(Money.of(300), result.get(0).refund());
        assertEquals(PaymentStatus.PARTIALLY_REFUNDED,
                app.payments.findByOccurrenceId(booking.occurrences().get(0).id()).get().status());
    }

    @Test
    void cancellingAtTheLastMomentRefundsNothingButStillFreesTheSlot() {
        Booking booking = paidBooking(at(Fixtures.TODAY, 11, 0), 60);    // 1 hour from "now"

        List<CancelledOccurrence> result = app.cancellationService.cancel(
                booking.id(), booking.occurrences().get(0).id(), CancellationScope.OCCURRENCE);

        assertEquals(Money.ZERO, result.get(0).refund());
        assertEquals(PaymentStatus.PAID, app.payments.findByOccurrenceId(booking.occurrences().get(0).id()).get().status());
        assertTrue(app.schedule.isFree("asha", slot(Fixtures.TODAY, 11, 0, 60)));
    }

    @Test
    void cancellingAnUnpaidOccurrenceHasNoRefund() {
        Booking booking = app.bookScheduled("asha", at(daysFromToday(3), 10, 0), 60);

        List<CancelledOccurrence> result = app.cancellationService.cancel(
                booking.id(), booking.occurrences().get(0).id(), CancellationScope.OCCURRENCE);

        assertEquals(Money.ZERO, result.get(0).refund());
    }

    @Test
    void cancellingFreesTheSlotSoItIsDiscoverableAndBookableAgain() {
        Booking booking = paidBooking(at(daysFromToday(3), 10, 0), 60);
        var criteria = SearchCriteria.builder().slot(slot(daysFromToday(3), 10, 0, 60)).build();
        assertTrue(app.search.search(criteria).isEmpty());

        app.cancellationService.cancel(booking.id(), booking.occurrences().get(0).id(), CancellationScope.OCCURRENCE);

        assertEquals(1, app.search.search(criteria).size());
        app.bookScheduled("asha", at(daysFromToday(3), 10, 0), 60);   // no SlotUnavailableException
    }

    @Test
    void cannotCancelTheSameOccurrenceTwice() {
        Booking booking = paidBooking(at(daysFromToday(3), 10, 0), 60);
        String occurrenceId = booking.occurrences().get(0).id();
        app.cancellationService.cancel(booking.id(), occurrenceId, CancellationScope.OCCURRENCE);

        assertThrows(InvalidStateTransitionException.class,
                () -> app.cancellationService.cancel(booking.id(), occurrenceId, CancellationScope.OCCURRENCE));
    }

    @Test
    void cannotCancelAnOccurrenceThatHasAlreadyStarted() {
        Booking booking = app.bookingService.book(new com.example.maid.booking.BookingRequest("c1", "asha", null,
                com.example.maid.domain.ServiceType.CLEANING, com.example.maid.domain.BookingType.INSTANT, null, 60, null));
        assertThrows(InvalidStateTransitionException.class, () -> app.cancellationService.cancel(
                booking.id(), booking.occurrences().get(0).id(), CancellationScope.OCCURRENCE));
    }

    @Test
    void unknownBookingOrOccurrenceIsReported() {
        Booking booking = paidBooking(at(daysFromToday(3), 10, 0), 60);
        assertThrows(NotFoundException.class,
                () -> app.cancellationService.cancel("nope", null, CancellationScope.SERIES));
        assertThrows(NotFoundException.class,
                () -> app.cancellationService.cancel(booking.id(), "nope", CancellationScope.OCCURRENCE));
    }

    @Test
    void singleOccurrenceCancellationNeedsAnOccurrenceId() {
        Booking booking = paidBooking(at(daysFromToday(3), 10, 0), 60);
        assertThrows(InvalidRequestException.class,
                () -> app.cancellationService.cancel(booking.id(), null, CancellationScope.OCCURRENCE));
    }

    // ---- recurring: single occurrence vs whole series ----

    private Booking weekdaySeries() {
        return app.bookRecurring("asha", at(daysFromToday(2), 9, 0), 60,      // Thu 3 Dec .. Wed 9 Dec
                RecurrenceRule.weekdays(LocalDate.of(2026, 12, 9)));
    }

    @Test
    void cancellingOneOccurrenceLeavesTheRestOfTheSeriesIntact() {
        Booking series = weekdaySeries();
        Occurrence second = series.occurrences().get(1);

        app.cancellationService.cancel(series.id(), second.id(), CancellationScope.OCCURRENCE);

        assertEquals(OccurrenceStatus.CANCELLED, second.status());
        assertEquals(OccurrenceStatus.PENDING_PAYMENT, series.occurrences().get(0).status());
        assertEquals(OccurrenceStatus.PENDING_PAYMENT, series.occurrences().get(2).status());
        assertEquals(BookingStatus.ACTIVE, series.status());
        assertTrue(app.schedule.isFree("asha", second.slot()));
        assertFalse(app.schedule.isFree("asha", series.occurrences().get(0).slot()));
    }

    @Test
    void cancellingTheSeriesCancelsEveryUpcomingOccurrenceAndRefundsEachPaidOne() {
        Booking series = weekdaySeries();
        app.paymentService.pay(app.cardPayment(series, 0, "k0"));
        app.paymentService.pay(app.cardPayment(series, 1, "k1"));

        List<CancelledOccurrence> result = app.cancellationService.cancel(series.id(), null, CancellationScope.SERIES);

        assertEquals(series.occurrences().size(), result.size());
        assertEquals(Money.of(300), result.get(0).refund());   // paid, > 24h away
        assertEquals(Money.of(300), result.get(1).refund());
        assertEquals(Money.ZERO, result.get(2).refund());      // never paid
        assertEquals(BookingStatus.CANCELLED, series.status());
        series.occurrences().forEach(o -> assertTrue(app.schedule.isFree("asha", o.slot())));
    }

    @Test
    void seriesCancellationSkipsOccurrencesAlreadyCancelled() {
        Booking series = weekdaySeries();
        app.cancellationService.cancel(series.id(), series.occurrences().get(0).id(), CancellationScope.OCCURRENCE);

        List<CancelledOccurrence> result = app.cancellationService.cancel(series.id(), null, CancellationScope.SERIES);

        assertEquals(series.occurrences().size() - 1, result.size());
    }

    @Test
    void cancellingAnAlreadyCancelledSeriesFails() {
        Booking series = weekdaySeries();
        app.cancellationService.cancel(series.id(), null, CancellationScope.SERIES);
        assertThrows(InvalidStateTransitionException.class,
                () -> app.cancellationService.cancel(series.id(), null, CancellationScope.SERIES));
    }

    @Test
    void scopeIsRequired() {
        Booking booking = paidBooking(at(daysFromToday(3), 10, 0), 60);
        assertThrows(InvalidRequestException.class, () -> app.cancellationService.cancel(booking.id(), null, null));
    }

    @Test
    void bookingTheFreedSlotOfACancelledOccurrenceDoesNotNeedAnythingElse() {
        Booking series = weekdaySeries();
        Occurrence first = series.occurrences().get(0);
        assertThrows(SlotUnavailableException.class, () -> app.bookScheduled("asha", first.slot().start(), 60));
        app.cancellationService.cancel(series.id(), first.id(), CancellationScope.OCCURRENCE);
        app.bookScheduled("asha", first.slot().start(), 60);
    }
}
