package com.example.maid;

import com.example.maid.booking.BookingRequest;
import com.example.maid.booking.BookingStrategy;
import com.example.maid.booking.InstantBookingStrategy;
import com.example.maid.booking.RecurrenceRule;
import com.example.maid.booking.RecurringBookingStrategy;
import com.example.maid.booking.ScheduledBookingStrategy;
import com.example.maid.cancellation.TimeBasedCancellationPolicy;
import com.example.maid.domain.Booking;
import com.example.maid.domain.BookingType;
import com.example.maid.domain.Maid;
import com.example.maid.domain.ServiceType;
import com.example.maid.payment.CardPaymentProcessor;
import com.example.maid.payment.PaymentProcessor;
import com.example.maid.payment.PaymentProcessorRegistry;
import com.example.maid.payment.PaymentRequest;
import com.example.maid.payment.UpiPaymentProcessor;
import com.example.maid.payment.WalletPaymentProcessor;
import com.example.maid.pricing.HourlyRatePricing;
import com.example.maid.repository.memory.InMemoryBookingRepository;
import com.example.maid.repository.memory.InMemoryMaidRepository;
import com.example.maid.repository.memory.InMemoryPaymentRepository;
import com.example.maid.repository.memory.InMemoryScheduleRepository;
import com.example.maid.search.AvailabilityFilter;
import com.example.maid.search.GenderFilter;
import com.example.maid.search.LocalityFilter;
import com.example.maid.search.MaidFilter;
import com.example.maid.search.MaidSearchService;
import com.example.maid.search.PriceFilter;
import com.example.maid.search.RatingFilter;
import com.example.maid.search.ServiceTypeFilter;
import com.example.maid.search.SkillsFilter;
import com.example.maid.service.BookingService;
import com.example.maid.service.CancellationService;
import com.example.maid.service.MaidService;
import com.example.maid.service.PaymentService;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/** Hand-wires the whole object graph with in-memory repositories, exactly as AppConfig does for Spring. */
public final class Wiring {

    public final Clock clock;
    public final InMemoryMaidRepository maids = new InMemoryMaidRepository();
    public final InMemoryBookingRepository bookings = new InMemoryBookingRepository();
    public final InMemoryPaymentRepository payments = new InMemoryPaymentRepository();
    public final InMemoryScheduleRepository schedule = new InMemoryScheduleRepository();
    public final MaidSearchService search;
    public final BookingService bookingService;
    public final PaymentService paymentService;
    public final CancellationService cancellationService;

    public Wiring() {
        this(Fixtures.clock(), List.of(new CardPaymentProcessor(), new UpiPaymentProcessor(),
                new WalletPaymentProcessor()));
    }

    public Wiring(Clock clock, List<PaymentProcessor> processors) {
        this.clock = clock;
        List<MaidFilter> filters = List.of(new LocalityFilter(), new ServiceTypeFilter(), new SkillsFilter(),
                new RatingFilter(), new PriceFilter(), new GenderFilter(), new AvailabilityFilter(schedule, clock));
        this.search = new MaidSearchService(maids, filters);
        List<BookingStrategy> strategies = List.of(new InstantBookingStrategy(), new ScheduledBookingStrategy(),
                new RecurringBookingStrategy());
        this.bookingService = new BookingService(new MaidService(maids), search, bookings, schedule,
                new HourlyRatePricing(), strategies, clock);
        this.paymentService = new PaymentService(bookings, payments, new PaymentProcessorRegistry(processors));
        this.cancellationService = new CancellationService(bookings, schedule, paymentService,
                new TimeBasedCancellationPolicy(), clock);
    }

    public Maid addMaid(Maid maid) {
        return maids.save(maid);
    }

    /** Scheduled one-off booking with a specific maid. */
    public Booking bookScheduled(String maidId, Instant start, int minutes) {
        return bookingService.book(new BookingRequest("cust-1", maidId, null, ServiceType.CLEANING,
                BookingType.SCHEDULED, start, minutes, null));
    }

    public Booking bookRecurring(String maidId, Instant start, int minutes, RecurrenceRule rule) {
        return bookingService.book(new BookingRequest("cust-1", maidId, null, ServiceType.CLEANING,
                BookingType.RECURRING, start, minutes, rule));
    }

    public PaymentRequest cardPayment(Booking booking, int occurrenceIndex, String key) {
        return new PaymentRequest(booking.id(), booking.occurrences().get(occurrenceIndex).id(),
                com.example.maid.domain.PaymentMethodType.CARD, "4111111111111111", key);
    }
}
