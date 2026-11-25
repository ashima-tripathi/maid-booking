package com.example.maid.domain;

import com.example.maid.exception.InvalidRequestException;

import java.time.ZoneId;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** A maid's profile and weekly working hours. Booked (busy) time is tracked separately by the schedule. */
public final class Maid {

    private final String id;
    private final String name;
    private final String locality;
    private final Set<ServiceType> skills;
    private final Money hourlyRate;
    private final double rating;
    private final Gender gender;
    private final List<AvailabilityWindow> availability;

    public Maid(String id, String name, String locality, Set<ServiceType> skills, Money hourlyRate,
                double rating, Gender gender, List<AvailabilityWindow> availability) {
        if (name == null || name.isBlank()) {
            throw new InvalidRequestException("Maid name is required");
        }
        if (locality == null || locality.isBlank()) {
            throw new InvalidRequestException("Maid locality is required");
        }
        if (skills == null || skills.isEmpty()) {
            throw new InvalidRequestException("Maid must offer at least one service");
        }
        if (availability == null || availability.isEmpty()) {
            throw new InvalidRequestException("Maid must have at least one availability window");
        }
        if (Objects.requireNonNull(hourlyRate, "hourlyRate").isZero()) {
            throw new InvalidRequestException("Hourly rate must be positive");
        }
        if (rating < 0 || rating > 5) {
            throw new InvalidRequestException("Rating must be between 0 and 5");
        }
        this.id = Objects.requireNonNull(id, "id");
        this.name = name.trim();
        this.locality = locality.trim();
        this.skills = Set.copyOf(EnumSet.copyOf(skills));
        this.hourlyRate = hourlyRate;
        this.rating = rating;
        this.gender = Objects.requireNonNull(gender, "gender");
        this.availability = List.copyOf(availability);
    }

    public boolean offers(ServiceType serviceType) {
        return skills.contains(serviceType);
    }

    /** True when the range falls entirely inside one of the maid's weekly windows. */
    public boolean isWorking(TimeRange range, ZoneId zone) {
        return availability.stream().anyMatch(window -> window.covers(range, zone));
    }

    public String id() { return id; }
    public String name() { return name; }
    public String locality() { return locality; }
    public Set<ServiceType> skills() { return skills; }
    public Money hourlyRate() { return hourlyRate; }
    public double rating() { return rating; }
    public Gender gender() { return gender; }
    public List<AvailabilityWindow> availability() { return availability; }
}
