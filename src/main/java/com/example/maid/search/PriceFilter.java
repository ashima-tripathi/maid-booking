package com.example.maid.search;

import com.example.maid.domain.Maid;

public class PriceFilter implements MaidFilter {

    @Override
    public boolean matches(Maid maid, SearchCriteria criteria) {
        return criteria.maxHourlyRate() == null || maid.hourlyRate().compareTo(criteria.maxHourlyRate()) <= 0;
    }
}
