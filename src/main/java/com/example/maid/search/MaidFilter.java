package com.example.maid.search;

import com.example.maid.domain.Maid;

/**
 * One search criterion. Adding a new filter means adding one implementation; the search service
 * simply applies every registered filter. A filter must return true when its criterion is absent.
 */
public interface MaidFilter {
    boolean matches(Maid maid, SearchCriteria criteria);
}
