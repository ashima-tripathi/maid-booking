package com.example.maid.config;

import com.example.maid.domain.AvailabilityWindow;
import com.example.maid.domain.Gender;
import com.example.maid.domain.Money;
import com.example.maid.domain.ServiceType;
import com.example.maid.service.MaidService;
import com.example.maid.service.NewMaid;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/** Seeds a few maids so the API can be tried straight away. Disable with maid.seed-demo-data=false. */
@Component
@ConditionalOnProperty(name = "maid.seed-demo-data", havingValue = "true", matchIfMissing = true)
public class DemoDataLoader implements CommandLineRunner {

    private final MaidService maidService;

    public DemoDataLoader(MaidService maidService) {
        this.maidService = maidService;
    }

    @Override
    public void run(String... args) {
        List<AvailabilityWindow> everyDay = Arrays.stream(DayOfWeek.values())
                .map(day -> new AvailabilityWindow(day, LocalTime.of(8, 0), LocalTime.of(20, 0)))
                .toList();
        maidService.onboard(new NewMaid("Asha", "Indiranagar", Set.of(ServiceType.CLEANING, ServiceType.DISHWASHING),
                Money.of("250"), 4.8, Gender.FEMALE, everyDay));
        maidService.onboard(new NewMaid("Meera", "Indiranagar", Set.of(ServiceType.CLEANING, ServiceType.COOKING),
                Money.of("300"), 4.5, Gender.FEMALE, everyDay));
        maidService.onboard(new NewMaid("Ravi", "Koramangala", Set.of(ServiceType.CLEANING),
                Money.of("200"), 4.2, Gender.MALE, everyDay));
    }
}
