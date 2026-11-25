package com.example.maid.domain;

import com.example.maid.exception.NotFoundException;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** A booking is a series of one or more occurrences with the same maid and service. */
public class Booking {

    private final String id;
    private final String customerId;
    private final String maidId;
    private final ServiceType serviceType;
    private final BookingType bookingType;
    private final List<Occurrence> occurrences;
    private final Instant createdAt;

    public Booking(String id, String customerId, String maidId, ServiceType serviceType,
                   BookingType bookingType, List<Occurrence> occurrences, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.customerId = Objects.requireNonNull(customerId, "customerId");
        this.maidId = Objects.requireNonNull(maidId, "maidId");
        this.serviceType = Objects.requireNonNull(serviceType, "serviceType");
        this.bookingType = Objects.requireNonNull(bookingType, "bookingType");
        this.occurrences = List.copyOf(occurrences);
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }

    public Occurrence findOccurrence(String occurrenceId) {
        return occurrences.stream()
                .filter(o -> o.id().equals(occurrenceId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Occurrence not found: " + occurrenceId));
    }

    /** Derived from the occurrences so it can never disagree with them. */
    public BookingStatus status() {
        if (occurrences.stream().anyMatch(Occurrence::isActive)) {
            return BookingStatus.ACTIVE;
        }
        boolean anyCompleted = occurrences.stream().anyMatch(o -> o.status() == OccurrenceStatus.COMPLETED);
        return anyCompleted ? BookingStatus.COMPLETED : BookingStatus.CANCELLED;
    }

    public String id() { return id; }
    public String customerId() { return customerId; }
    public String maidId() { return maidId; }
    public ServiceType serviceType() { return serviceType; }
    public BookingType bookingType() { return bookingType; }
    public List<Occurrence> occurrences() { return occurrences; }
    public Instant createdAt() { return createdAt; }
}
