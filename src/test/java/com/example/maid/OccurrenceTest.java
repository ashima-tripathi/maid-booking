package com.example.maid;

import com.example.maid.domain.Money;
import com.example.maid.domain.Occurrence;
import com.example.maid.domain.OccurrenceStatus;
import com.example.maid.exception.InvalidStateTransitionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.example.maid.Fixtures.daysFromToday;
import static com.example.maid.Fixtures.slot;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OccurrenceTest {

    private Occurrence occurrence;

    @BeforeEach
    void setUp() {
        occurrence = new Occurrence("o1", slot(daysFromToday(2), 10, 0, 60), Money.of(300));
    }

    @Test
    void startsPendingPayment() {
        assertEquals(OccurrenceStatus.PENDING_PAYMENT, occurrence.status());
        assertTrue(occurrence.isActive());
    }

    @Test
    void happyPathPendingToConfirmedToCompleted() {
        occurrence.confirmPayment();
        assertEquals(OccurrenceStatus.CONFIRMED, occurrence.status());
        occurrence.complete();
        assertEquals(OccurrenceStatus.COMPLETED, occurrence.status());
        assertFalse(occurrence.isActive());
    }

    @Test
    void cancelReportsThePreviousStatus() {
        assertEquals(OccurrenceStatus.PENDING_PAYMENT, occurrence.cancel());
        assertEquals(OccurrenceStatus.CANCELLED, occurrence.status());
    }

    @Test
    void cancellingAConfirmedOccurrenceReportsConfirmed() {
        occurrence.confirmPayment();
        assertEquals(OccurrenceStatus.CONFIRMED, occurrence.cancel());
    }

    @Test
    void cannotCancelTwice() {
        occurrence.cancel();
        assertThrows(InvalidStateTransitionException.class, occurrence::cancel);
    }

    @Test
    void cannotPayAfterCancelling() {
        occurrence.cancel();
        assertThrows(InvalidStateTransitionException.class, occurrence::confirmPayment);
    }

    @Test
    void cannotPayTwice() {
        occurrence.confirmPayment();
        assertThrows(InvalidStateTransitionException.class, occurrence::confirmPayment);
    }

    @Test
    void cannotCompleteBeforePayment() {
        assertThrows(InvalidStateTransitionException.class, occurrence::complete);
    }

    @Test
    void cannotCancelACompletedOccurrence() {
        occurrence.confirmPayment();
        occurrence.complete();
        assertThrows(InvalidStateTransitionException.class, occurrence::cancel);
    }
}
