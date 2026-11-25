package com.example.maid.service;

import com.example.maid.booking.BookingRequest;
import com.example.maid.booking.BookingStrategy;
import com.example.maid.domain.Booking;
import com.example.maid.domain.BookingType;
import com.example.maid.domain.Maid;
import com.example.maid.domain.Occurrence;
import com.example.maid.domain.TimeRange;
import com.example.maid.exception.InvalidRequestException;
import com.example.maid.exception.NotFoundException;
import com.example.maid.exception.SlotUnavailableException;
import com.example.maid.pricing.PricingStrategy;
import com.example.maid.repository.BookingRepository;
import com.example.maid.repository.ScheduleRepository;
import com.example.maid.search.MaidSearchService;
import com.example.maid.search.SearchCriteria;

import java.time.Clock;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Orchestrates a booking: resolve slots (per type) -> pick/validate maid -> reserve atomically ->
 * price -> persist. Everything type-specific is delegated to a BookingStrategy.
 */
public class BookingService {

    private final MaidService maidService;
    private final MaidSearchService searchService;
    private final BookingRepository bookingRepository;
    private final ScheduleRepository scheduleRepository;
    private final PricingStrategy pricingStrategy;
    private final Map<BookingType, BookingStrategy> strategies = new EnumMap<>(BookingType.class);
    private final Clock clock;

    public BookingService(MaidService maidService, MaidSearchService searchService,
                          BookingRepository bookingRepository, ScheduleRepository scheduleRepository,
                          PricingStrategy pricingStrategy, List<BookingStrategy> strategies, Clock clock) {
        this.maidService = maidService;
        this.searchService = searchService;
        this.bookingRepository = bookingRepository;
        this.scheduleRepository = scheduleRepository;
        this.pricingStrategy = pricingStrategy;
        this.clock = clock;
        strategies.forEach(strategy -> this.strategies.put(strategy.type(), strategy));
    }

    public Booking book(BookingRequest request) {
        validate(request);
        BookingStrategy strategy = strategies.get(request.bookingType());
        if (strategy == null) {
            throw new InvalidRequestException("Unsupported booking type: " + request.bookingType());
        }
        List<TimeRange> slots = strategy.resolveSlots(request, clock);

        Maid maid = request.maidId() != null
                ? reserveRequestedMaid(request, slots)
                : reserveBestAvailableMaid(request, slots);

        return bookingRepository.save(createBooking(request, maid, slots));
    }

    public Booking get(String bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found: " + bookingId));
    }

    private Maid reserveRequestedMaid(BookingRequest request, List<TimeRange> slots) {
        Maid maid = maidService.get(request.maidId());
        if (!isEligible(maid, request, slots)) {
            throw new SlotUnavailableException(
                    "Maid does not offer " + request.serviceType() + " during the requested time");
        }
        scheduleRepository.reserve(maid.id(), slots);
        return maid;
    }

    /**
     * Tries candidates best-first and relies on the atomic reserve (not on the search result) to win the slot,
     * so a maid who gets booked between search and reserve is simply skipped.
     */
    private Maid reserveBestAvailableMaid(BookingRequest request, List<TimeRange> slots) {
        SearchCriteria criteria = SearchCriteria.builder()
                .locality(request.locality())
                .serviceType(request.serviceType())
                .slot(slots.get(0))
                .build();
        for (Maid candidate : searchService.search(criteria)) {
            if (!isEligible(candidate, request, slots)) {
                continue;
            }
            try {
                scheduleRepository.reserve(candidate.id(), slots);
                return candidate;
            } catch (SlotUnavailableException conflict) {
                // someone else took this maid first; try the next candidate
            }
        }
        throw new SlotUnavailableException("No maid is available for the requested time");
    }

    private boolean isEligible(Maid maid, BookingRequest request, List<TimeRange> slots) {
        return maid.offers(request.serviceType())
                && slots.stream().allMatch(slot -> maid.isWorking(slot, clock.getZone()));
    }

    private Booking createBooking(BookingRequest request, Maid maid, List<TimeRange> slots) {
        List<Occurrence> occurrences = slots.stream()
                .map(slot -> new Occurrence(UUID.randomUUID().toString(), slot, pricingStrategy.priceFor(maid, slot)))
                .toList();
        return new Booking(UUID.randomUUID().toString(), request.customerId(), maid.id(),
                request.serviceType(), request.bookingType(), occurrences, clock.instant());
    }

    private void validate(BookingRequest request) {
        if (request.customerId() == null || request.customerId().isBlank()) {
            throw new InvalidRequestException("Customer id is required");
        }
        if (request.serviceType() == null) {
            throw new InvalidRequestException("Service type is required");
        }
        if (request.bookingType() == null) {
            throw new InvalidRequestException("Booking type is required");
        }
    }
}
