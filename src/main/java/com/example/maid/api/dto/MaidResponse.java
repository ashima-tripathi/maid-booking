package com.example.maid.api.dto;

import com.example.maid.domain.AvailabilityWindow;
import com.example.maid.domain.Gender;
import com.example.maid.domain.Maid;
import com.example.maid.domain.ServiceType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public record MaidResponse(String id, String name, String locality, Set<ServiceType> skills,
                           BigDecimal hourlyRate, double rating, Gender gender,
                           List<AvailabilityWindow> availability) {

    public static MaidResponse from(Maid maid) {
        return new MaidResponse(maid.id(), maid.name(), maid.locality(), maid.skills(),
                maid.hourlyRate().amount(), maid.rating(), maid.gender(), maid.availability());
    }
}
