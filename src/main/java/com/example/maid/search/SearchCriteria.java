package com.example.maid.search;

import com.example.maid.domain.Gender;
import com.example.maid.domain.Money;
import com.example.maid.domain.ServiceType;
import com.example.maid.domain.TimeRange;

import java.util.Set;

/** Every field is optional; a filter ignores criteria that were not supplied. */
public record SearchCriteria(String locality, ServiceType serviceType, Set<ServiceType> requiredSkills,
                             TimeRange slot, Double minRating, Money maxHourlyRate, Gender gender) {

    public SearchCriteria {
        requiredSkills = requiredSkills == null ? Set.of() : Set.copyOf(requiredSkills);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String locality;
        private ServiceType serviceType;
        private Set<ServiceType> requiredSkills;
        private TimeRange slot;
        private Double minRating;
        private Money maxHourlyRate;
        private Gender gender;

        public Builder locality(String value) { this.locality = value; return this; }
        public Builder serviceType(ServiceType value) { this.serviceType = value; return this; }
        public Builder requiredSkills(Set<ServiceType> value) { this.requiredSkills = value; return this; }
        public Builder slot(TimeRange value) { this.slot = value; return this; }
        public Builder minRating(Double value) { this.minRating = value; return this; }
        public Builder maxHourlyRate(Money value) { this.maxHourlyRate = value; return this; }
        public Builder gender(Gender value) { this.gender = value; return this; }

        public SearchCriteria build() {
            return new SearchCriteria(locality, serviceType, requiredSkills, slot, minRating, maxHourlyRate, gender);
        }
    }
}
