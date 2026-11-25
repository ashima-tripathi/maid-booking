package com.example.maid.search;

import com.example.maid.domain.Maid;
import com.example.maid.repository.ScheduleRepository;

import java.time.Clock;

/** Keeps only maids who work during the requested slot and have nothing booked in it. */
public class AvailabilityFilter implements MaidFilter {

    private final ScheduleRepository scheduleRepository;
    private final Clock clock;

    public AvailabilityFilter(ScheduleRepository scheduleRepository, Clock clock) {
        this.scheduleRepository = scheduleRepository;
        this.clock = clock;
    }

    @Override
    public boolean matches(Maid maid, SearchCriteria criteria) {
        if (criteria.slot() == null) {
            return true;
        }
        return maid.isWorking(criteria.slot(), clock.getZone())
                && scheduleRepository.isFree(maid.id(), criteria.slot());
    }
}
