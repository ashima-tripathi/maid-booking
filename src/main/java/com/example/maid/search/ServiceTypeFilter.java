package com.example.maid.search;

import com.example.maid.domain.Maid;

public class ServiceTypeFilter implements MaidFilter {

    @Override
    public boolean matches(Maid maid, SearchCriteria criteria) {
        return criteria.serviceType() == null || maid.offers(criteria.serviceType());
    }
}
