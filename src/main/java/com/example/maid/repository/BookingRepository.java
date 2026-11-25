package com.example.maid.repository;

import com.example.maid.domain.Booking;

import java.util.Optional;

public interface BookingRepository {
    Booking save(Booking booking);

    Optional<Booking> findById(String id);
}
