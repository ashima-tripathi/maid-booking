package com.example.maid;

import com.example.maid.domain.AvailabilityWindow;
import com.example.maid.domain.Gender;
import com.example.maid.domain.Maid;
import com.example.maid.domain.Money;
import com.example.maid.domain.ServiceType;
import com.example.maid.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static com.example.maid.Fixtures.ZONE;
import static com.example.maid.Fixtures.at;
import static com.example.maid.Fixtures.daysFromToday;
import static com.example.maid.Fixtures.slot;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MaidTest {

    // Wednesday 2026-12-02, working 09:00-13:00 only on Wednesdays
    private final Maid wednesdayMornings = new Maid("m1", "Asha", "Indiranagar", Set.of(ServiceType.CLEANING),
            Money.of(250), 4.5, Gender.FEMALE,
            List.of(new AvailabilityWindow(DayOfWeek.WEDNESDAY, LocalTime.of(9, 0), LocalTime.of(13, 0))));

    @Test
    void workingInsideWindow() {
        assertTrue(wednesdayMornings.isWorking(slot(daysFromToday(1), 9, 0, 120), ZONE));
        assertTrue(wednesdayMornings.isWorking(slot(daysFromToday(1), 11, 0, 120), ZONE));   // ends exactly at 13:00
    }

    @Test
    void notWorkingWhenSlotSpillsPastWindow() {
        assertFalse(wednesdayMornings.isWorking(slot(daysFromToday(1), 12, 0, 120), ZONE));
        assertFalse(wednesdayMornings.isWorking(slot(daysFromToday(1), 8, 30, 60), ZONE));
    }

    @Test
    void notWorkingOnOtherDays() {
        assertFalse(wednesdayMornings.isWorking(slot(daysFromToday(2), 10, 0, 60), ZONE));   // Thursday
    }

    @Test
    void slotCrossingMidnightIsNeverCovered() {
        assertFalse(wednesdayMornings.isWorking(slot(daysFromToday(1), 23, 0, 120), ZONE));
    }

    @Test
    void windowMustEndAfterItStarts() {
        assertThrows(InvalidRequestException.class,
                () -> new AvailabilityWindow(DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(10, 0)));
    }

    @Test
    void maidProfileIsValidated() {
        List<AvailabilityWindow> windows = Fixtures.everyDay8to20();
        Set<ServiceType> skills = Set.of(ServiceType.COOKING);
        assertThrows(InvalidRequestException.class,
                () -> new Maid("m", " ", "Indiranagar", skills, Money.of(100), 4, Gender.FEMALE, windows));
        assertThrows(InvalidRequestException.class,
                () -> new Maid("m", "Asha", "Indiranagar", Set.of(), Money.of(100), 4, Gender.FEMALE, windows));
        assertThrows(InvalidRequestException.class,
                () -> new Maid("m", "Asha", "Indiranagar", skills, Money.of(100), 4, Gender.FEMALE, List.of()));
        assertThrows(InvalidRequestException.class,
                () -> new Maid("m", "Asha", "Indiranagar", skills, Money.of(100), 5.5, Gender.FEMALE, windows));
        assertThrows(InvalidRequestException.class,
                () -> new Maid("m", "Asha", "Indiranagar", skills, Money.ZERO, 4, Gender.FEMALE, windows));
    }
}
