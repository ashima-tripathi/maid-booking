package com.example.maid.search;

import com.example.maid.domain.Maid;

public class GenderFilter implements MaidFilter {

    @Override
    public boolean matches(Maid maid, SearchCriteria criteria) {
        return criteria.gender() == null || maid.gender() == criteria.gender();
    }
}
