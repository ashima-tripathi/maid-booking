package com.example.maid.repository;

import com.example.maid.domain.Maid;

import java.util.Collection;
import java.util.Optional;

public interface MaidRepository {
    Maid save(Maid maid);

    Optional<Maid> findById(String id);

    Collection<Maid> findAll();
}
