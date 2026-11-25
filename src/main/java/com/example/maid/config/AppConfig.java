package com.example.maid.config;

import com.example.maid.booking.BookingStrategy;
import com.example.maid.booking.InstantBookingStrategy;
import com.example.maid.booking.RecurringBookingStrategy;
import com.example.maid.booking.ScheduledBookingStrategy;
import com.example.maid.cancellation.CancellationPolicy;
import com.example.maid.cancellation.TimeBasedCancellationPolicy;
import com.example.maid.payment.CardPaymentProcessor;
import com.example.maid.payment.PaymentProcessor;
import com.example.maid.payment.PaymentProcessorRegistry;
import com.example.maid.payment.UpiPaymentProcessor;
import com.example.maid.payment.WalletPaymentProcessor;
import com.example.maid.pricing.HourlyRatePricing;
import com.example.maid.pricing.PricingStrategy;
import com.example.maid.repository.BookingRepository;
import com.example.maid.repository.MaidRepository;
import com.example.maid.repository.PaymentRepository;
import com.example.maid.repository.ScheduleRepository;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;
import java.util.List;

/**
 * The only place that knows which implementations are in use. Core classes are plain Java and take their
 * collaborators through constructors, so swapping any piece (a JPA repository, a real gateway) is a change here.
 * Strategies, filters and processors are injected as lists: register another bean and it is picked up.
 */
@Configuration
public class AppConfig {

    @Bean
    Clock clock(@Value("${maid.zone:Asia/Kolkata}") String zone) {
        return Clock.system(ZoneId.of(zone));
    }

    // ---- persistence (swap for database-backed implementations) ----
    @Bean MaidRepository maidRepository() { return new InMemoryMaidRepository(); }
    @Bean BookingRepository bookingRepository() { return new InMemoryBookingRepository(); }
    @Bean PaymentRepository paymentRepository() { return new InMemoryPaymentRepository(); }
    @Bean ScheduleRepository scheduleRepository() { return new InMemoryScheduleRepository(); }

    // ---- search filters ----
    @Bean MaidFilter localityFilter() { return new LocalityFilter(); }
    @Bean MaidFilter serviceTypeFilter() { return new ServiceTypeFilter(); }
    @Bean MaidFilter skillsFilter() { return new SkillsFilter(); }
    @Bean MaidFilter ratingFilter() { return new RatingFilter(); }
    @Bean MaidFilter priceFilter() { return new PriceFilter(); }
    @Bean MaidFilter genderFilter() { return new GenderFilter(); }

    @Bean
    MaidFilter availabilityFilter(ScheduleRepository scheduleRepository, Clock clock) {
        return new AvailabilityFilter(scheduleRepository, clock);
    }

    // ---- booking types ----
    @Bean BookingStrategy instantBookingStrategy() { return new InstantBookingStrategy(); }
    @Bean BookingStrategy scheduledBookingStrategy() { return new ScheduledBookingStrategy(); }
    @Bean BookingStrategy recurringBookingStrategy() { return new RecurringBookingStrategy(); }

    // ---- payment methods ----
    @Bean PaymentProcessor cardPaymentProcessor() { return new CardPaymentProcessor(); }
    @Bean PaymentProcessor upiPaymentProcessor() { return new UpiPaymentProcessor(); }
    @Bean PaymentProcessor walletPaymentProcessor() { return new WalletPaymentProcessor(); }

    @Bean
    PaymentProcessorRegistry paymentProcessorRegistry(List<PaymentProcessor> processors) {
        return new PaymentProcessorRegistry(processors);
    }

    // ---- policies ----
    @Bean PricingStrategy pricingStrategy() { return new HourlyRatePricing(); }
    @Bean CancellationPolicy cancellationPolicy() { return new TimeBasedCancellationPolicy(); }

    // ---- services ----
    @Bean
    MaidService maidService(MaidRepository maidRepository) {
        return new MaidService(maidRepository);
    }

    @Bean
    MaidSearchService maidSearchService(MaidRepository maidRepository, List<MaidFilter> filters) {
        return new MaidSearchService(maidRepository, filters);
    }

    @Bean
    BookingService bookingService(MaidService maidService, MaidSearchService searchService,
                                  BookingRepository bookingRepository, ScheduleRepository scheduleRepository,
                                  PricingStrategy pricingStrategy, List<BookingStrategy> strategies, Clock clock) {
        return new BookingService(maidService, searchService, bookingRepository, scheduleRepository,
                pricingStrategy, strategies, clock);
    }

    @Bean
    PaymentService paymentService(BookingRepository bookingRepository, PaymentRepository paymentRepository,
                                  PaymentProcessorRegistry registry) {
        return new PaymentService(bookingRepository, paymentRepository, registry);
    }

    @Bean
    CancellationService cancellationService(BookingRepository bookingRepository, ScheduleRepository scheduleRepository,
                                            PaymentService paymentService, CancellationPolicy policy, Clock clock) {
        return new CancellationService(bookingRepository, scheduleRepository, paymentService, policy, clock);
    }
}
