package com.example.maid.service;

import com.example.maid.domain.AvailabilityWindow;
import com.example.maid.domain.Gender;
import com.example.maid.domain.Money;
import com.example.maid.domain.ServiceType;

import java.util.List;
import java.util.Set;

/** Onboarding details. rating is optional (defaults to 0) until real reviews exist. */
public record NewMaid(String name, String locality, Set<ServiceType> skills, Money hourlyRate,
                      Double rating, Gender gender, List<AvailabilityWindow> availability) {
}
