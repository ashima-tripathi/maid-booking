package com.example.maid.search;

import com.example.maid.domain.Maid;

public class RatingFilter implements MaidFilter {

    @Override
    public boolean matches(Maid maid, SearchCriteria criteria) {
        return criteria.minRating() == null || maid.rating() >= criteria.minRating();
    }
}
