package com.example.maid;

import com.example.maid.domain.TimeRange;
import com.example.maid.exception.SlotUnavailableException;
import com.example.maid.repository.memory.InMemoryScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.example.maid.Fixtures.daysFromToday;
import static com.example.maid.Fixtures.slot;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScheduleRepositoryTest {

    private InMemoryScheduleRepository schedule;
    private final TimeRange nineToEleven = slot(daysFromToday(1), 9, 0, 120);

    @BeforeEach
    void setUp() {
        schedule = new InMemoryScheduleRepository();
    }

    @Test
    void reservedSlotIsNoLongerFree() {
        schedule.reserve("m1", List.of(nineToEleven));
        assertFalse(schedule.isFree("m1", nineToEleven));
    }

    @Test
    void overlappingReservationIsRejected() {
        schedule.reserve("m1", List.of(nineToEleven));
        assertThrows(SlotUnavailableException.class,
                () -> schedule.reserve("m1", List.of(slot(daysFromToday(1), 10, 0, 60))));
    }

    @Test
    void adjacentReservationIsAllowed() {
        schedule.reserve("m1", List.of(nineToEleven));
        schedule.reserve("m1", List.of(slot(daysFromToday(1), 11, 0, 60)));
        assertFalse(schedule.isFree("m1", slot(daysFromToday(1), 11, 0, 60)));
    }

    @Test
    void differentMaidsDoNotInterfere() {
        schedule.reserve("m1", List.of(nineToEleven));
        assertTrue(schedule.isFree("m2", nineToEleven));
        schedule.reserve("m2", List.of(nineToEleven));
    }

    @Test
    void reservingSeveralSlotsIsAllOrNothing() {
        TimeRange taken = slot(daysFromToday(2), 9, 0, 60);
        TimeRange free = slot(daysFromToday(1), 9, 0, 60);
        schedule.reserve("m1", List.of(taken));

        assertThrows(SlotUnavailableException.class, () -> schedule.reserve("m1", List.of(free, taken)));

        assertTrue(schedule.isFree("m1", free));   // the first slot was not left reserved
    }

    @Test
    void overlappingSlotsWithinOneRequestAreRejected() {
        assertThrows(SlotUnavailableException.class, () -> schedule.reserve("m1",
                List.of(slot(daysFromToday(1), 9, 0, 120), slot(daysFromToday(1), 10, 0, 60))));
    }

    @Test
    void releasingFreesTheSlot() {
        schedule.reserve("m1", List.of(nineToEleven));
        schedule.release("m1", nineToEleven);
        assertTrue(schedule.isFree("m1", nineToEleven));
        schedule.reserve("m1", List.of(nineToEleven));
    }

    @Test
    void onlyOneOfManyConcurrentReservationsWins() throws Exception {
        int threads = 20;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger conflicts = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                ready.countDown();
                try {
                    go.await();
                    schedule.reserve("m1", List.of(nineToEleven));
                    successes.incrementAndGet();
                } catch (SlotUnavailableException e) {
                    conflicts.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        ready.await();
        go.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));

        assertEquals(1, successes.get());
        assertEquals(threads - 1, conflicts.get());
    }
}
