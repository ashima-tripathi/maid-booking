package com.example.maid.search;

import com.example.maid.domain.Maid;
import com.example.maid.repository.MaidRepository;

import java.util.Comparator;
import java.util.List;

public class MaidSearchService {

    private static final Comparator<Maid> BEST_FIRST = Comparator
            .comparingDouble(Maid::rating).reversed()
            .thenComparing(Maid::hourlyRate)
            .thenComparing(Maid::name);

    private final MaidRepository maidRepository;
    private final List<MaidFilter> filters;

    public MaidSearchService(MaidRepository maidRepository, List<MaidFilter> filters) {
        this.maidRepository = maidRepository;
        this.filters = List.copyOf(filters);
    }

    /** Matching maids, best rated first, then cheapest. */
    public List<Maid> search(SearchCriteria criteria) {
        return maidRepository.findAll().stream()
                .filter(maid -> filters.stream().allMatch(filter -> filter.matches(maid, criteria)))
                .sorted(BEST_FIRST)
                .toList();
    }
}
