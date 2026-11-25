package com.example.maid.search;

import com.example.maid.domain.Maid;

public class LocalityFilter implements MaidFilter {

    @Override
    public boolean matches(Maid maid, SearchCriteria criteria) {
        return criteria.locality() == null || maid.locality().equalsIgnoreCase(criteria.locality());
    }
}
