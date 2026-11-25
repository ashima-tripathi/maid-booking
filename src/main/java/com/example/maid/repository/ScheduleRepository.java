package com.example.maid.repository;

import com.example.maid.domain.TimeRange;

import java.util.List;

/**
 * Tracks the time a maid is already committed. This is the single place that guards shared inventory,
 * so a database-backed implementation only has to honour the same atomic reserve contract
 * (for example with a unique/exclusion constraint on maid and time range).
 */
public interface ScheduleRepository {

    /** Atomically reserves all slots or none. Throws SlotUnavailableException on any conflict. */
    void reserve(String maidId, List<TimeRange> slots);

    void release(String maidId, TimeRange slot);

    boolean isFree(String maidId, TimeRange slot);
}
