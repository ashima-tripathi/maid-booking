package com.example.maid;

import com.example.maid.booking.BookingRequest;
import com.example.maid.booking.RecurrenceRule;
import com.example.maid.domain.Booking;
import com.example.maid.domain.BookingStatus;
import com.example.maid.domain.BookingType;
import com.example.maid.domain.Gender;
import com.example.maid.domain.Money;
import com.example.maid.domain.OccurrenceStatus;
import com.example.maid.domain.ServiceType;
import com.example.maid.exception.InvalidRequestException;
import com.example.maid.exception.NotFoundException;
import com.example.maid.exception.SlotUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static com.example.maid.Fixtures.at;
import static com.example.maid.Fixtures.daysFromToday;
import static com.example.maid.Fixtures.slot;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookingServiceTest {

    private Wiring app;

    @BeforeEach
    void setUp() {
        app = new Wiring();
        app.addMaid(Fixtures.cleaner("asha"));        // 300/hour, rating 4.5
    }

    @Test
    void scheduledBookingIsPricedByDurationAndStartsPendingPayment() {
        Booking booking = app.bookScheduled("asha", at(daysFromToday(1), 10, 0), 120);

        assertEquals("asha", booking.maidId());
        assertEquals(BookingType.SCHEDULED, booking.bookingType());
        assertEquals(1, booking.occurrences().size());
        assertEquals(Money.of(600), booking.occurrences().get(0).price());
        assertEquals(OccurrenceStatus.PENDING_PAYMENT, booking.occurrences().get(0).status());
        assertEquals(BookingStatus.ACTIVE, booking.status());
    }

    @Test
    void bookingCanBeFetchedAfterwards() {
        Booking booking = app.bookScheduled("asha", at(daysFromToday(1), 10, 0), 60);
        assertEquals(booking.id(), app.bookingService.get(booking.id()).id());
        assertThrows(NotFoundException.class, () -> app.bookingService.get("missing"));
    }

    @Test
    void instantBookingStartsNow() {
        Booking booking = app.bookingService.book(new BookingRequest("c1", "asha", null, ServiceType.CLEANING,
                BookingType.INSTANT, null, 60, null));
        assertEquals(Fixtures.NOW, booking.occurrences().get(0).slot().start());
    }

    @Test
    void doubleBookingTheSameSlotIsRejected() {
        app.bookScheduled("asha", at(daysFromToday(1), 10, 0), 120);
        assertThrows(SlotUnavailableException.class,
                () -> app.bookScheduled("asha", at(daysFromToday(1), 11, 0), 60));
    }

    @Test
    void backToBackBookingsAreAllowed() {
        app.bookScheduled("asha", at(daysFromToday(1), 10, 0), 60);
        app.bookScheduled("asha", at(daysFromToday(1), 11, 0), 60);
    }

    @Test
    void recurringBookingCreatesOneOccurrencePerDate() {
        Booking booking = app.bookRecurring("asha", at(daysFromToday(1), 9, 0), 60,
                RecurrenceRule.weekdays(LocalDate.of(2026, 12, 8)));
        assertEquals(5, booking.occurrences().size());
        assertEquals(Money.of(300), booking.occurrences().get(4).price());
    }

    @Test
    void recurringBookingBlocksEveryOccurrence() {
        app.bookRecurring("asha", at(daysFromToday(1), 9, 0), 60, RecurrenceRule.weekdays(LocalDate.of(2026, 12, 8)));
        // Monday 7 Dec 09:30 clashes with the Monday 09:00-10:00 occurrence
        assertThrows(SlotUnavailableException.class,
                () -> app.bookScheduled("asha", at(LocalDate.of(2026, 12, 7), 9, 30), 30));
    }

    @Test
    void recurringBookingIsAllOrNothingWhenOneDateClashes() {
        app.bookScheduled("asha", at(LocalDate.of(2026, 12, 4), 9, 0), 60);     // Friday is taken

        assertThrows(SlotUnavailableException.class, () -> app.bookRecurring("asha", at(daysFromToday(1), 9, 0), 60,
                RecurrenceRule.weekdays(LocalDate.of(2026, 12, 8))));

        // Wednesday (the first date of the failed series) must still be free
        app.bookScheduled("asha", at(daysFromToday(1), 9, 0), 60);
    }

    @Test
    void maidMustOfferTheRequestedService() {
        assertThrows(SlotUnavailableException.class, () -> app.bookingService.book(new BookingRequest("c1", "asha",
                null, ServiceType.COOKING, BookingType.SCHEDULED, at(daysFromToday(1), 10, 0), 60, null)));
    }

    @Test
    void bookingOutsideWorkingHoursIsRejected() {
        assertThrows(SlotUnavailableException.class,
                () -> app.bookScheduled("asha", at(daysFromToday(1), 19, 30), 60));   // runs past 20:00
    }

    @Test
    void unknownMaidIsRejected() {
        assertThrows(NotFoundException.class, () -> app.bookScheduled("ghost", at(daysFromToday(1), 10, 0), 60));
    }

    @Test
    void requestWithoutCustomerIsRejected() {
        assertThrows(InvalidRequestException.class, () -> app.bookingService.book(new BookingRequest(" ", "asha",
                null, ServiceType.CLEANING, BookingType.SCHEDULED, at(daysFromToday(1), 10, 0), 60, null)));
    }

    // ---- automatic maid selection ----

    @Test
    void withoutAMaidIdTheBestRatedAvailableMaidInTheLocalityIsChosen() {
        app.addMaid(Fixtures.maid("meera", "Indiranagar", 4.9, 350, Gender.FEMALE, ServiceType.CLEANING));
        app.addMaid(Fixtures.maid("ravi", "Koramangala", 5.0, 200, Gender.MALE, ServiceType.CLEANING));

        Booking booking = autoBook("Indiranagar", at(daysFromToday(1), 10, 0));

        assertEquals("meera", booking.maidId());    // Ravi is better rated but in another locality
    }

    @Test
    void autoSelectionMovesOnToTheNextMaidWhenTheBestIsBusy() {
        app.addMaid(Fixtures.maid("meera", "Indiranagar", 4.9, 350, Gender.FEMALE, ServiceType.CLEANING));
        autoBook("Indiranagar", at(daysFromToday(1), 10, 0));                       // takes Meera
        Booking second = autoBook("Indiranagar", at(daysFromToday(1), 10, 0));     // same slot

        assertEquals("asha", second.maidId());
    }

    @Test
    void autoSelectionFailsWhenNobodyIsFree() {
        autoBook("Indiranagar", at(daysFromToday(1), 10, 0));
        assertThrows(SlotUnavailableException.class, () -> autoBook("Indiranagar", at(daysFromToday(1), 10, 0)));
    }

    @Test
    void autoSelectedRecurringSeriesGoesToOneMaidThatIsFreeForTheWholeSeries() {
        app.addMaid(Fixtures.maid("meera", "Indiranagar", 4.9, 350, Gender.FEMALE, ServiceType.CLEANING));
        app.bookScheduled("meera", at(LocalDate.of(2026, 12, 4), 9, 0), 60);   // Meera is busy on one date

        Booking series = app.bookingService.book(new BookingRequest("c1", null, "Indiranagar",
                ServiceType.CLEANING, BookingType.RECURRING, at(daysFromToday(1), 9, 0), 60,
                RecurrenceRule.weekdays(LocalDate.of(2026, 12, 8))));

        assertEquals("asha", series.maidId());
        assertEquals(5, series.occurrences().size());
        assertFalse(series.occurrences().isEmpty());
    }

    private Booking autoBook(String locality, java.time.Instant start) {
        return app.bookingService.book(new BookingRequest("c1", null, locality, ServiceType.CLEANING,
                BookingType.SCHEDULED, start, 60, null));
    }
}
