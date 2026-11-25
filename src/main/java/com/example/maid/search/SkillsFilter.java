package com.example.maid.search;

import com.example.maid.domain.Maid;

public class SkillsFilter implements MaidFilter {

    @Override
    public boolean matches(Maid maid, SearchCriteria criteria) {
        return maid.skills().containsAll(criteria.requiredSkills());
    }
}
