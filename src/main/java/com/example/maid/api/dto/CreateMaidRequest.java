package com.example.maid.api.dto;

import com.example.maid.domain.AvailabilityWindow;
import com.example.maid.domain.Gender;
import com.example.maid.domain.Money;
import com.example.maid.domain.ServiceType;
import com.example.maid.service.NewMaid;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

public record CreateMaidRequest(
        @NotBlank String name,
        @NotBlank String locality,
        @NotEmpty Set<ServiceType> skills,
        @NotNull @Positive BigDecimal hourlyRate,
        @DecimalMin("0") @DecimalMax("5") Double rating,
        @NotNull Gender gender,
        @NotEmpty List<@Valid @NotNull WindowDto> availability) {

    public record WindowDto(@NotNull DayOfWeek day, @NotNull LocalTime from, @NotNull LocalTime to) {
    }

    public NewMaid toCommand() {
        List<AvailabilityWindow> windows = availability.stream()
                .map(w -> new AvailabilityWindow(w.day(), w.from(), w.to()))
                .toList();
        return new NewMaid(name, locality, skills, new Money(hourlyRate), rating, gender, windows);
    }
}
