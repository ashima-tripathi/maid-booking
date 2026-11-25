package com.example.maid;

import com.example.maid.domain.Gender;
import com.example.maid.domain.Maid;
import com.example.maid.domain.Money;
import com.example.maid.domain.ServiceType;
import com.example.maid.domain.TimeRange;
import com.example.maid.search.MaidFilter;
import com.example.maid.search.MaidSearchService;
import com.example.maid.search.SearchCriteria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static com.example.maid.Fixtures.daysFromToday;
import static com.example.maid.Fixtures.slot;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MaidSearchServiceTest {

    private Wiring app;

    @BeforeEach
    void setUp() {
        app = new Wiring();
        app.addMaid(Fixtures.maid("asha", "Indiranagar", 4.8, 250, Gender.FEMALE, ServiceType.CLEANING, ServiceType.DISHWASHING));
        app.addMaid(Fixtures.maid("meera", "Indiranagar", 4.5, 300, Gender.FEMALE, ServiceType.CLEANING, ServiceType.COOKING));
        app.addMaid(Fixtures.maid("ravi", "Koramangala", 4.2, 200, Gender.MALE, ServiceType.CLEANING));
    }

    private List<String> ids(SearchCriteria criteria) {
        return app.search.search(criteria).stream().map(Maid::id).toList();
    }

    @Test
    void noCriteriaReturnsEveryoneBestRatedFirst() {
        assertEquals(List.of("asha", "meera", "ravi"), ids(SearchCriteria.builder().build()));
    }

    @Test
    void filtersByLocality() {
        assertEquals(List.of("ravi"), ids(SearchCriteria.builder().locality("koramangala").build()));
    }

    @Test
    void filtersByServiceType() {
        assertEquals(List.of("meera"), ids(SearchCriteria.builder().serviceType(ServiceType.COOKING).build()));
    }

    @Test
    void filtersByAllRequiredSkills() {
        SearchCriteria criteria = SearchCriteria.builder()
                .requiredSkills(Set.of(ServiceType.CLEANING, ServiceType.DISHWASHING)).build();
        assertEquals(List.of("asha"), ids(criteria));
    }

    @Test
    void filtersByMinimumRating() {
        assertEquals(List.of("asha", "meera"), ids(SearchCriteria.builder().minRating(4.5).build()));
    }

    @Test
    void filtersByMaxHourlyRate() {
        assertEquals(List.of("asha", "ravi"), ids(SearchCriteria.builder().maxHourlyRate(Money.of(250)).build()));
    }

    @Test
    void filtersByGender() {
        assertEquals(List.of("ravi"), ids(SearchCriteria.builder().gender(Gender.MALE).build()));
    }

    @Test
    void combinesFilters() {
        SearchCriteria criteria = SearchCriteria.builder()
                .locality("Indiranagar").serviceType(ServiceType.CLEANING).maxHourlyRate(Money.of(280)).build();
        assertEquals(List.of("asha"), ids(criteria));
    }

    @Test
    void excludesMaidsAlreadyBookedInTheRequestedSlot() {
        TimeRange tenToTwelve = slot(daysFromToday(1), 10, 0, 120);
        app.bookScheduled("asha", tenToTwelve.start(), 120);

        List<String> available = ids(SearchCriteria.builder().slot(tenToTwelve).build());

        assertEquals(List.of("meera", "ravi"), available);
    }

    @Test
    void anOverlappingSlotAlsoExcludesTheBusyMaid() {
        app.bookScheduled("asha", slot(daysFromToday(1), 10, 0, 120).start(), 120);
        List<String> available = ids(SearchCriteria.builder().slot(slot(daysFromToday(1), 11, 0, 120)).build());
        assertEquals(List.of("meera", "ravi"), available);
    }

    @Test
    void excludesMaidsOutsideTheirWorkingHours() {
        TimeRange lateNight = slot(daysFromToday(1), 21, 0, 60);
        assertTrue(ids(SearchCriteria.builder().slot(lateNight).build()).isEmpty());
    }

    @Test
    void aNewFilterPlugsInWithoutChangingTheSearchService() {
        MaidFilter onlyNamedMeera = (maid, criteria) -> maid.id().equals("meera");
        MaidSearchService custom = new MaidSearchService(app.maids, List.of(onlyNamedMeera));
        assertEquals(1, custom.search(SearchCriteria.builder().build()).size());
    }
}
