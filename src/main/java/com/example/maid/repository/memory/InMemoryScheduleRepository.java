package com.example.maid.repository.memory;

import com.example.maid.domain.TimeRange;
import com.example.maid.exception.SlotUnavailableException;
import com.example.maid.repository.ScheduleRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * One lock per maid: bookings for different maids never contend, and a check-then-reserve for the
 * same maid is atomic. A booking only ever touches one maid, so there is no lock-ordering problem.
 */
public class InMemoryScheduleRepository implements ScheduleRepository {

    private static final class MaidSchedule {
        final ReentrantLock lock = new ReentrantLock();
        final List<TimeRange> reserved = new ArrayList<>();

        boolean conflictsWith(TimeRange slot) {
            return reserved.stream().anyMatch(slot::overlaps);
        }
    }

    private final ConcurrentMap<String, MaidSchedule> schedules = new ConcurrentHashMap<>();

    @Override
    public void reserve(String maidId, List<TimeRange> slots) {
        MaidSchedule schedule = scheduleOf(maidId);
        schedule.lock.lock();
        try {
            for (int i = 0; i < slots.size(); i++) {
                TimeRange slot = slots.get(i);
                if (schedule.conflictsWith(slot) || overlapsEarlierRequestedSlot(slots, i)) {
                    throw new SlotUnavailableException("Maid is not available at " + slot.start());
                }
            }
            schedule.reserved.addAll(slots);   // validated every slot first, so this is all-or-nothing
        } finally {
            schedule.lock.unlock();
        }
    }

    @Override
    public void release(String maidId, TimeRange slot) {
        MaidSchedule schedule = scheduleOf(maidId);
        schedule.lock.lock();
        try {
            schedule.reserved.remove(slot);
        } finally {
            schedule.lock.unlock();
        }
    }

    @Override
    public boolean isFree(String maidId, TimeRange slot) {
        MaidSchedule schedule = scheduleOf(maidId);
        schedule.lock.lock();
        try {
            return !schedule.conflictsWith(slot);
        } finally {
            schedule.lock.unlock();
        }
    }

    private boolean overlapsEarlierRequestedSlot(List<TimeRange> slots, int index) {
        for (int j = 0; j < index; j++) {
            if (slots.get(j).overlaps(slots.get(index))) {
                return true;
            }
        }
        return false;
    }

    private MaidSchedule scheduleOf(String maidId) {
        return schedules.computeIfAbsent(maidId, id -> new MaidSchedule());
    }
}
