package com.example.maid.repository.memory;

import com.example.maid.domain.Maid;
import com.example.maid.repository.MaidRepository;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryMaidRepository implements MaidRepository {

    private final Map<String, Maid> maids = new ConcurrentHashMap<>();

    @Override
    public Maid save(Maid maid) {
        maids.put(maid.id(), maid);
        return maid;
    }

    @Override
    public Optional<Maid> findById(String id) {
        return Optional.ofNullable(maids.get(id));
    }

    @Override
    public Collection<Maid> findAll() {
        return maids.values();
    }
}
