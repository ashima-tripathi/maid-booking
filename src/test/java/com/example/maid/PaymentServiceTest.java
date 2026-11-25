package com.example.maid;

import com.example.maid.domain.Booking;
import com.example.maid.domain.Money;
import com.example.maid.domain.OccurrenceStatus;
import com.example.maid.domain.Payment;
import com.example.maid.domain.PaymentMethodType;
import com.example.maid.domain.PaymentStatus;
import com.example.maid.exception.InvalidRequestException;
import com.example.maid.exception.InvalidStateTransitionException;
import com.example.maid.exception.PaymentFailedException;
import com.example.maid.payment.CardPaymentProcessor;
import com.example.maid.payment.GatewayResult;
import com.example.maid.payment.PaymentProcessor;
import com.example.maid.payment.PaymentRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static com.example.maid.Fixtures.at;
import static com.example.maid.Fixtures.daysFromToday;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PaymentServiceTest {

    /** Counts real charges so idempotency can be asserted. */
    private static final class CountingCardProcessor extends CardPaymentProcessor {
        final AtomicInteger charges = new AtomicInteger();

        @Override
        public GatewayResult charge(Money amount, String instrument, String idempotencyKey) {
            charges.incrementAndGet();
            return super.charge(amount, instrument, idempotencyKey);
        }
    }

    private CountingCardProcessor card;
    private Wiring app;
    private Booking booking;

    @BeforeEach
    void setUp() {
        card = new CountingCardProcessor();
        app = new Wiring(Fixtures.clock(), List.of(card, new com.example.maid.payment.UpiPaymentProcessor()));
        app.addMaid(Fixtures.cleaner("asha"));
        booking = app.bookScheduled("asha", at(daysFromToday(3), 10, 0), 120);
    }

    @Test
    void successfulPaymentConfirmsTheOccurrence() {
        Payment payment = app.paymentService.pay(app.cardPayment(booking, 0, "key-1"));

        assertEquals(PaymentStatus.PAID, payment.status());
        assertEquals(Money.of(600), payment.amount());
        assertEquals(OccurrenceStatus.CONFIRMED, booking.occurrences().get(0).status());
    }

    @Test
    void retryWithTheSameKeyReturnsTheOriginalPaymentAndDoesNotChargeAgain() {
        Payment first = app.paymentService.pay(app.cardPayment(booking, 0, "key-1"));
        Payment retry = app.paymentService.pay(app.cardPayment(booking, 0, "key-1"));

        assertSame(first, retry);
        assertEquals(1, card.charges.get());
    }

    @Test
    void reusingAKeyForADifferentOccurrenceIsRejected() {
        Booking other = app.bookScheduled("asha", at(daysFromToday(4), 10, 0), 60);
        app.paymentService.pay(app.cardPayment(booking, 0, "key-1"));
        assertThrows(InvalidRequestException.class, () -> app.paymentService.pay(app.cardPayment(other, 0, "key-1")));
    }

    @Test
    void payingTwiceWithDifferentKeysIsRejected() {
        app.paymentService.pay(app.cardPayment(booking, 0, "key-1"));
        assertThrows(InvalidStateTransitionException.class,
                () -> app.paymentService.pay(app.cardPayment(booking, 0, "key-2")));
        assertEquals(1, card.charges.get());
    }

    @Test
    void declinedPaymentLeavesTheOccurrencePending() {
        PaymentRequest declined = new PaymentRequest(booking.id(), booking.occurrences().get(0).id(),
                PaymentMethodType.CARD, "4111111111110000", "key-1");

        assertThrows(PaymentFailedException.class, () -> app.paymentService.pay(declined));
        assertEquals(OccurrenceStatus.PENDING_PAYMENT, booking.occurrences().get(0).status());

        // the customer can try again with a working card
        app.paymentService.pay(app.cardPayment(booking, 0, "key-2"));
        assertEquals(OccurrenceStatus.CONFIRMED, booking.occurrences().get(0).status());
    }

    @Test
    void invalidInstrumentIsRejectedByTheProcessor() {
        PaymentRequest bad = new PaymentRequest(booking.id(), booking.occurrences().get(0).id(),
                PaymentMethodType.UPI, "not-a-upi-id", "key-1");
        assertThrows(PaymentFailedException.class, () -> app.paymentService.pay(bad));
    }

    @Test
    void anotherMethodWorksThroughTheSameAbstraction() {
        PaymentRequest upi = new PaymentRequest(booking.id(), booking.occurrences().get(0).id(),
                PaymentMethodType.UPI, "asha@upi", "key-1");
        assertEquals(PaymentMethodType.UPI, app.paymentService.pay(upi).method());
    }

    @Test
    void unregisteredPaymentMethodIsRejected() {
        PaymentRequest wallet = new PaymentRequest(booking.id(), booking.occurrences().get(0).id(),
                PaymentMethodType.WALLET, "w-1", "key-1");   // this wiring has no wallet processor
        assertThrows(InvalidRequestException.class, () -> app.paymentService.pay(wallet));
    }

    @Test
    void idempotencyKeyIsRequired() {
        assertThrows(InvalidRequestException.class, () -> app.paymentService.pay(app.cardPayment(booking, 0, " ")));
    }

    @Test
    void cannotPayForACancelledOccurrence() {
        booking.occurrences().get(0).cancel();
        assertThrows(InvalidStateTransitionException.class,
                () -> app.paymentService.pay(app.cardPayment(booking, 0, "key-1")));
    }

    @Test
    void eachOccurrenceOfARecurringBookingIsPaidSeparately() {
        Booking series = app.bookRecurring("asha", at(daysFromToday(5), 9, 0), 60,
                com.example.maid.booking.RecurrenceRule.weekly(java.time.DayOfWeek.SUNDAY, java.time.LocalDate.of(2026, 12, 20)));
        app.paymentService.pay(app.cardPayment(series, 0, "k-0"));

        assertEquals(OccurrenceStatus.CONFIRMED, series.occurrences().get(0).status());
        assertEquals(OccurrenceStatus.PENDING_PAYMENT, series.occurrences().get(1).status());
    }
}
