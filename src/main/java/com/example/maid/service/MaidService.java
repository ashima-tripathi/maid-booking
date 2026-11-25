package com.example.maid.service;

import com.example.maid.domain.Maid;
import com.example.maid.exception.NotFoundException;
import com.example.maid.repository.MaidRepository;

import java.util.UUID;

public class MaidService {

    private final MaidRepository maidRepository;

    public MaidService(MaidRepository maidRepository) {
        this.maidRepository = maidRepository;
    }

    /** Validation lives in the Maid constructor, so an invalid maid can never be stored. */
    public Maid onboard(NewMaid request) {
        Maid maid = new Maid(UUID.randomUUID().toString(), request.name(), request.locality(), request.skills(),
                request.hourlyRate(), request.rating() == null ? 0.0 : request.rating(),
                request.gender(), request.availability());
        return maidRepository.save(maid);
    }

    public Maid get(String maidId) {
        return maidRepository.findById(maidId)
                .orElseThrow(() -> new NotFoundException("Maid not found: " + maidId));
    }
}
